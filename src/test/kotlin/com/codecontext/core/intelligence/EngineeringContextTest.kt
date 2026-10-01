package com.codecontext.core.intelligence

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EngineeringContextTest : StringSpec({
    "diff detects added removed and modified files deterministically" {
        val before = EngineeringContextSnapshot(
            ENGINEERING_CONTEXT_SCHEMA_VERSION, "a", listOf(
                ContextFile("a.java", "111", 1),
                ContextFile("b.java", "222", 2),
                ContextFile("c.java", "333", 3)
            ), 3, 6, listOf("Java"), false, emptyList(), "before"
        )
        val after = EngineeringContextSnapshot(
            ENGINEERING_CONTEXT_SCHEMA_VERSION, "b", listOf(
                ContextFile("a.java", "999", 1),
                ContextFile("b.java", "222", 2),
                ContextFile("d.java", "444", 4)
            ), 3, 7, listOf("Java"), true, listOf("a.java"), "after"
        )

        val diff = EngineeringContextEngine.diff(before, after)

        diff.summary.added shouldBe 1
        diff.summary.removed shouldBe 1
        diff.summary.modified shouldBe 1
        diff.summary.unchanged shouldBe 1
        diff.changes.map { it.path } shouldBe listOf("a.java", "c.java", "d.java")
        diff.changes.map { it.type } shouldBe listOf("MODIFIED", "REMOVED", "ADDED")
    }

    "identical snapshots produce no changes" {
        val snapshot = EngineeringContextSnapshot(
            ENGINEERING_CONTEXT_SCHEMA_VERSION, "a", listOf(ContextFile("a.java", "111", 1)),
            1, 1, listOf("Java"), false, emptyList(), "digest"
        )
        val diff = EngineeringContextEngine.diff(snapshot, snapshot)
        diff.summary shouldBe ContextDiffSummary(0, 0, 0, 1)
        diff.changes shouldBe emptyList()
    }
})
