package dev.mikepenz.a2cui.codegen

import kotlin.test.Test
import kotlin.test.assertTrue

class PromptGeneratorTest {

    @Test
    fun includesNameDescriptionPropsAndEvents() {
        val spec = ComponentSpec(
            name = "Rating",
            description = "5-star rating",
            factoryReference = "com.example.RatingFactory",
            props = listOf(
                PropSpec("value", PropType.INTEGER, "Current 0-5", required = true, defaultValue = "0", enumValues = emptyList()),
            ),
            events = listOf(EventSpec("onChange", "Rating changed", emptyList())),
            slots = emptyList(),
        )
        val prompt = PromptGenerator.generate("my-catalog", listOf(spec))
        assertTrue("Rating" in prompt)
        assertTrue("5-star rating" in prompt)
        assertTrue("Current 0-5" in prompt)
        assertTrue("onChange" in prompt)
        assertTrue("my-catalog" in prompt)
        assertTrue("required" in prompt)
    }

    @Test
    fun sortsComponentsAlphabetically() {
        val a = blank("Alpha")
        val b = blank("Bravo")
        val z = blank("Zeta")
        val prompt = PromptGenerator.generate("c", listOf(z, a, b))
        val aIdx = prompt.indexOf("## Alpha")
        val bIdx = prompt.indexOf("## Bravo")
        val zIdx = prompt.indexOf("## Zeta")
        assertTrue(aIdx in 0 until bIdx)
        assertTrue(bIdx in 0 until zIdx)
    }

    private fun blank(name: String) = ComponentSpec(name, "", "x.$name", emptyList(), emptyList(), emptyList())
}
