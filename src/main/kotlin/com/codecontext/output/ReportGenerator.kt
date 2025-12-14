package com.codecontext.output

import com.codecontext.core.generator.LearningStep
import com.codecontext.core.graph.DependencyGraph
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
        // Git Metadata Fields
        val authors: String = "",
        val churn: Int = 0,
        val lastMod: String = "",
        val description: String = "" // Gap 1: Context
)

@Serializable data class GraphLink(val source: String, val target: String, val value: Int = 1)

@Serializable data class GraphData(val nodes: List<GraphNode>, val links: List<GraphLink>)

class ReportGenerator {
    fun generate(
            graph: DependencyGraph,
            outputPath: String,
            parsedFiles: List<com.codecontext.core.parser.ParsedFile>,
            learningPath: List<LearningStep>
    ) {
        val hotspots = graph.getTopHotspots(15)

        // Create map for easy lookup of Git data
        val fileMap = parsedFiles.associateBy { it.file.absolutePath }

        // Gap 3: Team Contribution Map
        val teamStats = mutableMapOf<String, Int>()
        parsedFiles.forEach { file ->
            file.gitMetadata.topAuthors.forEach { author ->
                teamStats[author] = (teamStats[author] ?: 0) + 1
            }
        }
        val topTeam = teamStats.entries.sortedByDescending { it.value }.take(10)

        // Convert graph data to JSON for visualization
        val nodes =
                graph.graph.vertexSet().map { id ->
                    val fileData = fileMap[id]
                    val meta = fileData?.gitMetadata
                    val authors = meta?.topAuthors?.joinToString(", ") ?: "Unknown"
                    val churn = meta?.changeFrequency ?: 0
                    val lastMod =
                            if (meta != null && meta.lastModified > 0)
                                    java.util.Date(meta.lastModified).toString()
                            else "Never"

                    val desc = fileData?.description ?: ""

                    GraphNode(
                            id = id,
                            label = File(id).name,
                            score = (graph.pageRankScores[id] ?: 0.0),
                            authors = authors,
                            churn = churn,
                            lastMod = lastMod,
                            description = desc
                    )
                }
        val links =
                graph.graph.edgeSet().map {
                    GraphLink(
                            source = graph.graph.getEdgeSource(it),
                            target = graph.graph.getEdgeTarget(it)
                    )
                }

        val graphData = GraphData(nodes, links)
        val jsonGraph = Json.encodeToString(graphData)

        val htmlContent =
                createHTML().html {
                    head {
                        title("CodeContext Analysis Report")
                        style {
                            unsafe {
                                +"""
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f4f4f4; }
                        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #333; }
                        h2 { color: #555; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-top: 30px; }
                        .hotspot-list { list-style: none; padding: 0; }
                        .hotspot-item { padding: 10px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; }
                        .hotspot-score { font-weight: bold; color: #e74c3c; }
                        .description { color: #666; font-style: italic; display: block; margin-top: 4px; }
                        .team-table { width: 100%; border-collapse: collapse; }
                        .team-table th, .team-table td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }
                        #graph-container { width: 100%; height: 600px; border: 1px solid #ddd; margin-top: 20px; }
                        """
                            }
                        }
                        script(src = "https://unpkg.com/force-graph") {}
                    }
                    body {
                        div("container") {
                            h1 { +"CodeContext Analysis Report" }

                            // Gap 3: Team Contribution
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
                                                span { +" [${step.description}]" } // Logic type

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
                                h2 { +"🗺️ Codebase Map (Hover for Smart Context)" }
                                div { id = "graph-container" }
                            }
                        }

                        script {
                            unsafe {
                                +"""
                        const gData = $jsonGraph;
                        
                        const Graph = ForceGraph()
                          (document.getElementById('graph-container'))
                            .graphData(gData)
                            .nodeLabel(node => {
                                let label = `${'$'}{node.label}\n\n`;
                                if (node.description) label += `💡 ${'$'}{node.description}\n\n`;
                                label += `Score: ${'$'}{node.score.toFixed(4)}\n`;
                                label += `Authors: ${'$'}{node.authors}\n`;
                                label += `Changes: ${'$'}{node.churn}\n`;
                                label += `Last Mod: ${'$'}{node.lastMod}`;
                                return label;
                            })
                            .nodeVal('score')
                            .nodeAutoColorBy('group')
                            .linkDirectionalParticles(2)
                            .linkDirectionalParticleWidth(2);
                        """
                            }
                        }
                    }
                }

        File(outputPath).writeText(htmlContent)
    }
}
