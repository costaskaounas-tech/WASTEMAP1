package gr.koukamedics.dispatcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun credentialsAndNonstandardPortsAreNotTrusted() {
        assertFalse(UrlPolicy.opensInsideApp("https://user@${UrlPolicy.APP_HOST}"))
        assertFalse(UrlPolicy.opensInsideApp("https://${UrlPolicy.APP_HOST}:8443"))
        assertTrue(UrlPolicy.opensInsideApp("https://${UrlPolicy.APP_HOST}:443/jobs"))
    }

    @Test
    fun dangerousOrMalformedExternalSchemesAreBlocked() {
        listOf("file:///data/data/private.db", "content://private/item", "javascript:alert(1)",
            "data:text/html,test", "intent://x", "https://", "http://user@example.com",
            "https://example.com:99999").forEach {
            assertFalse(it, UrlPolicy.canOpenExternal(it))
        }
    }

    @Test
    fun supportedExternalLinksAreAllowed() {
        listOf("https://maps.google.com/?q=Athens", "tel:+302100000000", "sms:+302100000000",
            "mailto:test@example.com", "viber://forward?text=test", "geo:0,0?q=Athens",
            "google.navigation:q=Athens").forEach {
            assertTrue(it, UrlPolicy.canOpenExternal(it))
        }
    }

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
