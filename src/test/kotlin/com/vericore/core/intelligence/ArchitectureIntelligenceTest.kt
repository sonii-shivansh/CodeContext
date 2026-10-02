package com.vericore.core.intelligence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

class ArchitectureIntelligenceTest : FunSpec({
    fun graph(vararg edges: Pair<String, String>): DefaultDirectedGraph<String, DefaultEdge> {
        val result = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
        edges.flatMap { listOf(it.first, it.second) }.distinct().forEach(result::addVertex)
        edges.forEach { result.addEdge(it.first, it.second) }
        return result
    }

    test("detects forbidden default layer direction") {
        val root = File("build/architecture-fixture")
        val controller = File(root, "src/main/kotlin/com/example/controller/PaymentController.kt").absolutePath
        val repository = File(root, "src/main/kotlin/com/example/repository/PaymentRepository.kt").absolutePath
        val result = ArchitectureIntelligenceEngine.analyze(graph(controller to repository), root)

        result.findings.map { it.ruleId } shouldContain "ARCH-LAYER-001"
        result.summary.crossLayerDependencies shouldBe 1
    }

    test("detects strongly connected architecture cycle") {
        val root = File("build/architecture-fixture")
        val a = File(root, "src/main/kotlin/com/example/service/A.kt").absolutePath
        val b = File(root, "src/main/kotlin/com/example/domain/B.kt").absolutePath
        val result = ArchitectureIntelligenceEngine.analyze(graph(a to b, b to a), root)

        result.cycles.size shouldBe 1
        result.findings.map { it.ruleId } shouldContain "ARCH-CYCLE-001"
    }

    test("produces deterministic findings") {
        val root = File("build/architecture-fixture")
        val a = File(root, "src/main/kotlin/com/example/controller/A.kt").absolutePath
        val b = File(root, "src/main/kotlin/com/example/repository/B.kt").absolutePath
        val c = File(root, "src/main/kotlin/com/example/domain/C.kt").absolutePath
        val first = ArchitectureIntelligenceEngine.analyze(graph(a to b, b to c), root)
        val second = ArchitectureIntelligenceEngine.analyze(graph(b to c, a to b), root)

        first.findings shouldContainExactly second.findings
        first.cycles shouldBe second.cycles
        first.layers shouldBe second.layers
    }
})
