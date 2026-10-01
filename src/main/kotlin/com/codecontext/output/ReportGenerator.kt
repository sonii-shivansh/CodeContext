package com.codecontext.output

import com.codecontext.core.generator.LearningStep
import com.codecontext.core.graph.RobustDependencyGraph
import java.io.File
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GraphNode(
        val id: String,
        val label: String,
        val score: Double,
        val group: Int = 1,
        val authors: String = "",
        val churn: Int = 0,
        val lastMod: String = "",
        val description: String = ""
)

@Serializable data class GraphLink(val source: String, val target: String, val value: Int = 1)

@Serializable data class GraphData(val nodes: List<GraphNode>, val links: List<GraphLink>)

class ReportGenerator {
    fun generate(
            graph: RobustDependencyGraph,
            outputPath: String,
            parsedFiles: List<com.codecontext.core.parser.ParsedFile>,
            learningPath: List<LearningStep>
    ) {
        val hotspots = graph.getTopHotspots(15)
        val fileMap = parsedFiles.associateBy { it.file.absolutePath }

        val teamStats = mutableMapOf<String, Int>()
        parsedFiles.forEach { file ->
            file.gitMetadata.topAuthors.forEach { author ->
                teamStats[author] = (teamStats[author] ?: 0) + 1
            }
        }
        val topTeam = teamStats.entries.sortedByDescending { it.value }.take(10)

        val nodes = graph.graph.vertexSet().map { id ->
            val fileData = fileMap[id]
            val meta = fileData?.gitMetadata
            val authors = meta?.topAuthors?.joinToString(", ") ?: "Unknown"
            val churn = meta?.changeFrequency ?: 0
            val lastMod = if (meta != null && meta.lastModified > 0) {
                java.util.Date(meta.lastModified).toString()
            } else {
                "Never"
            }
            GraphNode(
                id = id,
                label = File(id).name,
                score = graph.pageRankScores[id] ?: 0.0,
                authors = authors,
                churn = churn,
                lastMod = lastMod,
                description = fileData?.description ?: ""
            )
        }
        val links = graph.graph.edgeSet().map {
            GraphLink(
                source = graph.graph.getEdgeSource(it),
                target = graph.graph.getEdgeTarget(it)
            )
        }

        // Escape characters that could terminate the surrounding HTML script element.
        val safeJsonGraph = Json.encodeToString(GraphData(nodes, links))
            .replace("<", "\\u003c")
            .replace(">", "\\u003e")
            .replace("&", "\\u0026")

        val htmlContent = createHTML().html {
            head {
                title("CodeContext Analysis Report")
                meta { charset = "utf-8" }
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                style {
                    unsafe {
                        +"""
                        :root { color-scheme: light; }
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f4f4f4; color: #222; }
                        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #333; }
                        h2 { color: #555; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-top: 30px; }
                        .hotspot-list { list-style: none; padding: 0; }
                        .hotspot-item { padding: 10px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; gap: 16px; }
                        .hotspot-score { font-weight: bold; color: #e74c3c; }
                        .description { color: #666; font-style: italic; display: block; margin-top: 4px; }
                        .team-table { width: 100%; border-collapse: collapse; }
                        .team-table th, .team-table td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }
                        #graph-container { position: relative; width: 100%; min-height: 420px; border: 1px solid #ddd; margin-top: 20px; overflow: hidden; background: #fbfbfb; }
                        #codebase-map { display: block; width: 100%; height: 420px; }
                        #graph-tooltip { position: absolute; display: none; max-width: 320px; padding: 10px 12px; border: 1px solid #ddd; border-radius: 6px; background: rgba(255,255,255,.97); box-shadow: 0 4px 16px rgba(0,0,0,.15); pointer-events: none; font-size: 13px; line-height: 1.4; }
                        @media (max-width: 640px) { body { padding: 10px; } .container { padding: 14px; } .hotspot-item { align-items: flex-start; } #codebase-map { height: 340px; } }
                        """
                    }
                }
            }
            body {
                div("container") {
                    h1 { +"CodeContext Analysis Report" }

                    div {
                        h2 { +"👥 Team Contribution Map" }
                        table("team-table") {
                            tr {
                                th { +"Developer" }
                                th { +"Files Modified" }
                            }
                            topTeam.forEach { (author, count) ->
                                tr {
                                    td { +author }
                                    td { +count.toString() }
                                }
                            }
                        }
                    }

                    div {
                        h2 { +"🎓 Personalized Learning Path" }
                        p { +"Start from these fundamental files and work your way up:" }
                        ul("hotspot-list") {
                            learningPath.forEach { step ->
                                li("hotspot-item") {
                                    div {
                                        strong { +File(step.file).name }
                                        span { +" [${step.description}]" }
                                        val fileDesc = fileMap[step.file]?.description
                                        if (!fileDesc.isNullOrBlank()) {
                                            span("description") { +"💡 $fileDesc" }
                                        }
                                        br {}
                                        small { +step.reason }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        h2 { +"🔥 Knowledge Hotspots (Top Critical Files)" }
                        ul("hotspot-list") {
                            hotspots.forEach { (path, score) ->
                                li("hotspot-item") {
                                    div {
                                        span { +File(path).name }
                                        val fileDesc = fileMap[path]?.description
                                        if (!fileDesc.isNullOrBlank()) {
                                            span("description") { +"💡 $fileDesc" }
                                        }
                                    }
                                    span("hotspot-score") { +String.format("%.4f", score) }
                                }
                            }
                        }
                    }

                    div {
                        h2 { +"🗺️ Codebase Map (hover for context)" }
                        div {
                            id = "graph-container"
                            canvas { id = "codebase-map" }
                            div { id = "graph-tooltip" }
                        }
                    }
                }

                script {
                    unsafe {
                        +"""
                        (() => {
                          const data = $safeJsonGraph;
                          const container = document.getElementById('graph-container');
                          const canvas = document.getElementById('codebase-map');
                          const tooltip = document.getElementById('graph-tooltip');
                          const ctx = canvas.getContext('2d');
                          let nodes = [];

                          function resize() {
                            const ratio = window.devicePixelRatio || 1;
                            const rect = container.getBoundingClientRect();
                            canvas.width = Math.max(1, Math.floor(rect.width * ratio));
                            canvas.height = Math.max(1, Math.floor(rect.height * ratio));
                            canvas.style.width = rect.width + 'px';
                            canvas.style.height = rect.height + 'px';
                            ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
                            draw();
                          }

                          function layout() {
                            const rect = container.getBoundingClientRect();
                            const cx = rect.width / 2;
                            const cy = rect.height / 2;
                            const radius = Math.max(60, Math.min(rect.width, rect.height) * .34);
                            nodes = data.nodes.map((node, index) => {
                              const angle = data.nodes.length ? (index / data.nodes.length) * Math.PI * 2 : 0;
                              return { ...node, x: cx + Math.cos(angle) * radius, y: cy + Math.sin(angle) * radius, r: Math.max(4, Math.min(14, 5 + node.score * 400)) };
                            });
                          }

                          function draw() {
                            const rect = container.getBoundingClientRect();
                            ctx.clearRect(0, 0, rect.width, rect.height);
                            const byId = new Map(nodes.map(n => [n.id, n]));
                            ctx.lineWidth = 1;
                            ctx.strokeStyle = '#b8b8b8';
                            data.links.forEach(link => {
                              const source = byId.get(link.source), target = byId.get(link.target);
                              if (!source || !target) return;
                              ctx.beginPath(); ctx.moveTo(source.x, source.y); ctx.lineTo(target.x, target.y); ctx.stroke();
                            });
                            nodes.forEach(node => {
                              ctx.beginPath();
                              ctx.fillStyle = '#2563eb';
                              ctx.arc(node.x, node.y, node.r, 0, Math.PI * 2);
                              ctx.fill();
                            });
                          }

                          function nodeAt(x, y) {
                            for (let i = nodes.length - 1; i >= 0; i--) {
                              const n = nodes[i], dx = x - n.x, dy = y - n.y;
                              if (dx * dx + dy * dy <= n.r * n.r + 36) return n;
                            }
                            return null;
                          }

                          canvas.addEventListener('mousemove', event => {
                            const rect = canvas.getBoundingClientRect();
                            const node = nodeAt(event.clientX - rect.left, event.clientY - rect.top);
                            if (!node) { tooltip.style.display = 'none'; return; }
                            tooltip.style.display = 'block';
                            tooltip.style.left = Math.min(event.clientX - rect.left + 12, rect.width - 330) + 'px';
                            tooltip.style.top = Math.max(8, event.clientY - rect.top + 12) + 'px';
                            tooltip.textContent = `${node.label} — score ${node.score.toFixed(4)}; changes ${node.churn}; authors ${node.authors || 'Unknown'}`;
                          });
                          canvas.addEventListener('mouseleave', () => { tooltip.style.display = 'none'; });
                          window.addEventListener('resize', () => { layout(); resize(); });
                          layout(); resize();
                        })();
                        """
                    }
                }
            }
        }

        File(outputPath).writeText(htmlContent)
    }
}
