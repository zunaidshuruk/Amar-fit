package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyRemoteTest {

    @Test
    fun extractSpotifyUri_returnsCorrectUriForOpenSpotifyLinks() {
        assertEquals(
            "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
            SpotifyRemote.extractSpotifyUri("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345")
        )
        assertEquals(
            "spotify:album:4aawyAB9vmqN3uQ7FjRGTy",
            SpotifyRemote.extractSpotifyUri("https://open.spotify.com/album/4aawyAB9vmqN3uQ7FjRGTy")
        )
        assertEquals(
            "spotify:track:11dFghVXANMlKmJXsNCbNl",
            SpotifyRemote.extractSpotifyUri("https://open.spotify.com/track/11dFghVXANMlKmJXsNCbNl?si=abc")
        )
    }

    @Test
    fun extractSpotifyUri_returnsUriUnchangedIfAlreadySpotifyScheme() {
        assertEquals(
            "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
            SpotifyRemote.extractSpotifyUri("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M")
        )
    }

    @Test
    fun extractSpotifyUri_returnsNullForInvalidOrNonSpotifyLinks() {
        assertNull(SpotifyRemote.extractSpotifyUri("https://music.youtube.com/playlist?list=PL123"))
        assertNull(SpotifyRemote.extractSpotifyUri("https://example.com/song"))
        assertNull(SpotifyRemote.extractSpotifyUri(""))
        assertNull(SpotifyRemote.extractSpotifyUri("   "))
    }
}
