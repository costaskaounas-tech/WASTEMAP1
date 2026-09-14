package gr.koukamedics.dispatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun exactHttpsHostOpensInsideApp() {
        assertTrue(
            UrlPolicy.opensInsideApp(
                "https://nursego-athens.costaskaounas.chatgpt.site/jobs?view=map#today",
            ),
        )
    }

    @Test
    fun lookalikeAndExternalHostsOpenOutsideApp() {
        assertFalse(
            UrlPolicy.opensInsideApp(
                "https://nursego-athens.costaskaounas.chatgpt.site.example.com",
            ),
        )
        assertFalse(UrlPolicy.opensInsideApp("https://maps.google.com"))
    }

    @Test
    fun nonHttpsSchemesOpenOutsideApp() {
        assertFalse(
            UrlPolicy.opensInsideApp(
                "http://nursego-athens.costaskaounas.chatgpt.site",
            ),
        )
        assertFalse(UrlPolicy.opensInsideApp("viber://forward"))
    }
}
