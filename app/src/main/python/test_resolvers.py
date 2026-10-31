import os
import tempfile
import unittest
from generic_media_resolver import (
    _clean_yt_dlp_error,
    _kind_from_entry,
    _mime_from_kind,
    _format_label,
    _is_muxer_compatible_video,
    _is_muxer_compatible_audio,
    _pick_audio_pair,
    _entry_formats,
    _entry_webpage_url,
    _flat_entry_payload,
    _ydl_options,
)
from instagram_resolver import (
    _build_title,
    _video_payload,
    _image_payload,
    _sidecar_node_payload,
)


class DummyObject:
    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            setattr(self, k, v)


class TestGenericMediaResolver(unittest.TestCase):

    def test_ydl_options_with_and_without_cookies(self):
        opts_plain = _ydl_options()
        self.assertNotIn("cookiefile", opts_plain)
        from generic_media_resolver import _DEFAULT_YOUTUBE_CLIENTS_ANONYMOUS, _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED
        self.assertEqual(opts_plain["extractor_args"]["youtube"]["player_client"], _DEFAULT_YOUTUBE_CLIENTS_ANONYMOUS)

        with tempfile.NamedTemporaryFile("w", delete=False) as f:
            f.write("# Netscape HTTP Cookie File\n")
            f_name = f.name

        try:
            opts_cookie = _ydl_options(cookie_file_path=f_name)
            self.assertEqual(opts_cookie["cookiefile"], f_name)
            self.assertEqual(opts_cookie["extractor_args"]["youtube"]["player_client"], _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED)
            self.assertIn("android", _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED)
            self.assertIn("web", _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED)
            self.assertLess(
                _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED.index("android"),
                _DEFAULT_YOUTUBE_CLIENTS_AUTHENTICATED.index("web")
            )
        finally:
            if os.path.exists(f_name):
                os.remove(f_name)

    def test_clean_yt_dlp_error(self):
        err1 = "ERROR: [youtube] dQw4w9WgXcQ: Sign in to confirm you're not a bot. See https://..."
        cleaned1 = _clean_yt_dlp_error(err1)
        self.assertEqual(cleaned1, "Sign in to confirm you're not a bot.")

        err2 = "[youtube] Unable to extract video data"
        cleaned2 = _clean_yt_dlp_error(err2)
        self.assertEqual(cleaned2, "Unable to extract video data")

        err3 = "Video unavailable"
        cleaned3 = _clean_yt_dlp_error(err3)
        self.assertEqual(cleaned3, "Video unavailable")

    def test_kind_and_mime_from_entry(self):
        self.assertEqual(_kind_from_entry({"vcodec": "avc1"}, "mp4"), "video")
        self.assertEqual(_kind_from_entry({"acodec": "mp4a"}, "m4a"), "audio")
        self.assertEqual(_kind_from_entry({}, "jpg"), "image")

        self.assertEqual(_mime_from_kind("video", "mp4"), "video/mp4")
        self.assertEqual(_mime_from_kind("audio", "m4a"), "audio/m4a")
        self.assertEqual(_mime_from_kind("image", "jpg"), "image/jpeg")

    def test_format_label(self):
        video_label = _format_label({}, "video", "mp4", 1080, 60, None)
        self.assertEqual(video_label, "1080p 60fps MP4")

        audio_label = _format_label({}, "audio", "m4a", None, None, 128)
        self.assertEqual(audio_label, "Audio 128 kbps M4A")

    def test_muxer_codec_compatibility(self):
        # Compatible video codecs for Android MediaMuxer
        self.assertTrue(_is_muxer_compatible_video("avc1.640028"))
        self.assertTrue(_is_muxer_compatible_video("h264"))
        self.assertTrue(_is_muxer_compatible_video("hev1.1.6.L93.B0"))
        self.assertTrue(_is_muxer_compatible_video("vp9"))
        self.assertTrue(_is_muxer_compatible_video("av01.0.08M.08"))

        # Incompatible video codec
        self.assertFalse(_is_muxer_compatible_video("unknown_codec_xyz"))

        # Compatible audio codecs for MediaMuxer and audio downloads
        self.assertTrue(_is_muxer_compatible_audio("mp4a.40.2"))
        self.assertTrue(_is_muxer_compatible_audio("aac"))
        self.assertTrue(_is_muxer_compatible_audio("opus"))
        self.assertTrue(_is_muxer_compatible_audio("flac"))
        self.assertFalse(_is_muxer_compatible_audio("unknown_audio_codec"))

    def test_pick_audio_pair(self):
        video_mp4 = {"ext": "mp4"}
        audio_m4a = {
            "ext": "m4a",
            "url": "http://example.com/audio.m4a",
            "abr": 128,
            "vcodec": "none",
            "acodec": "mp4a",
        }
        audio_opus = {
            "ext": "opus",
            "url": "http://example.com/audio.opus",
            "abr": 160,
            "vcodec": "none",
            "acodec": "opus",
        }

        # MP4 video should prefer MP4 audio family (m4a/aac)
        pair = _pick_audio_pair(video_mp4, [audio_m4a, audio_opus])
        self.assertEqual(pair["ext"], "m4a")

        # WebM video should prefer WebM audio family (opus/webm)
        video_webm = {"ext": "webm"}
        pair_webm = _pick_audio_pair(video_webm, [audio_m4a, audio_opus])
        self.assertEqual(pair_webm["ext"], "opus")

        # GoogleVideo (YouTube): MP4 video must pair with itag 140 (m4a), WebM video must pair with itag 251 (opus)
        audio_yt_140 = {
            "format_id": "140-drc",
            "ext": "m4a",
            "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback",
            "abr": 129,
            "vcodec": "none",
            "acodec": "mp4a.40.2",
        }
        audio_yt_251 = {
            "format_id": "251-drc",
            "ext": "webm",
            "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback",
            "abr": 114,
            "vcodec": "none",
            "acodec": "opus",
        }
        video_yt_mp4 = {
            "ext": "mp4",
            "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback",
        }
        video_yt_webm = {
            "ext": "webm",
            "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback",
        }
        yt_pair_mp4 = _pick_audio_pair(video_yt_mp4, [audio_yt_140, audio_yt_251])
        self.assertEqual(yt_pair_mp4["format_id"], "140-drc")
        self.assertEqual(yt_pair_mp4["ext"], "m4a")

        yt_pair_webm = _pick_audio_pair(video_yt_webm, [audio_yt_140, audio_yt_251])
        self.assertEqual(yt_pair_webm["format_id"], "251-drc")
        self.assertEqual(yt_pair_webm["ext"], "webm")

    def test_entry_webpage_url_with_full_and_flat_id(self):
        # Entry with full webpage_url
        self.assertEqual(
            _entry_webpage_url({"webpage_url": "https://www.youtube.com/watch?v=123"}),
            "https://www.youtube.com/watch?v=123",
        )
        # Entry with full url
        self.assertEqual(
            _entry_webpage_url({"url": "https://www.youtube.com/watch?v=456"}),
            "https://www.youtube.com/watch?v=456",
        )
        # Flat entry with bare ID in 'url' and 'id'
        self.assertEqual(
            _entry_webpage_url({"id": "dQw4w9WgXcQ", "url": "dQw4w9WgXcQ", "ie_key": "Youtube"}),
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        )
        # Flat entry with bare ID in 'id' only
        self.assertEqual(
            _entry_webpage_url({"id": "abc789"}),
            "https://www.youtube.com/watch?v=abc789",
        )
        # Entry with relative /watch URL
        self.assertEqual(
            _entry_webpage_url({"url": "/watch?v=rel123"}),
            "https://www.youtube.com/watch?v=rel123",
        )

    def test_entry_formats_pairing_and_deduplication(self):
        entry = {
            "formats": [
                {
                    "format_id": "137",
                    "url": "http://example.com/1080p.mp4",
                    "vcodec": "avc1.640028",
                    "acodec": "none",
                    "ext": "mp4",
                    "height": 1080,
                    "tbr": 4000,
                    "protocol": "https",
                },
                {
                    "format_id": "140",
                    "url": "http://example.com/audio.m4a",
                    "vcodec": "none",
                    "acodec": "mp4a.40.2",
                    "ext": "m4a",
                    "abr": 128,
                    "protocol": "https",
                },
                {
                    "format_id": "18",
                    "url": "http://example.com/360p.mp4",
                    "vcodec": "avc1.42001E",
                    "acodec": "mp4a.40.2",
                    "ext": "mp4",
                    "height": 360,
                    "tbr": 600,
                    "protocol": "https",
                },
            ]
        }
        formats = _entry_formats(entry)
        self.assertTrue(len(formats) >= 2)

        # 1080p video-only format should have merge_audio attached
        fmt_1080p = next(f for f in formats if f.get("height") == 1080)
        self.assertIsNotNone(fmt_1080p.get("merge_audio"))
        self.assertEqual(fmt_1080p["merge_audio"]["ext"], "m4a")

    def test_entry_webpage_url(self):
        entry_yt = {"id": "dQw4w9WgXcQ", "extractor_key": "Youtube"}
        self.assertEqual(_entry_webpage_url(entry_yt), "https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        entry_web = {"webpage_url": "https://vimeo.com/12345"}
        self.assertEqual(_entry_webpage_url(entry_web), "https://vimeo.com/12345")

    def test_playlist_all_five_videos_flat_entries(self):
        # All 5 videos from YouTube playlist PLPs-gzgoaa4c
        playlist_raw_entries = [
            {
                "id": "8v0s64d1Sic",
                "title": "Claude for Beginners: How to Actually Use It (Full Guide)",
                "duration": 1512,
                "uploader": "AI Channel",
                "ie_key": "Youtube",
            },
            {
                "id": "_i-MHQDaBR4",
                "title": "How to Use Claude AI: Complete Beginner's Guide (Maya's First Week)",
                "duration": 422,
                "uploader": "AI Channel",
                "ie_key": "Youtube",
            },
            {
                "id": "bwPCZhNGTIw",
                "title": "Claude Tutorial for Beginners (Step by Step, Free Plan)",
                "duration": 1569,
                "uploader": "AI Channel",
                "ie_key": "Youtube",
            },
            {
                "id": "7HQ4FWVwAsk",
                "title": "Full Claude Tutorial for Beginners - Become A Pro In Just 30 Minutes!",
                "duration": 1915,
                "uploader": "AI Channel",
                "ie_key": "Youtube",
            },
            {
                "id": "2nQO0y1NvqU",
                "title": "The Ultimate Beginner's Guide to Claude AI",
                "duration": 137,
                "uploader": "AI Channel",
                "ie_key": "Youtube",
            },
        ]

        payloads = [_flat_entry_payload(e) for e in playlist_raw_entries]
        self.assertEqual(len(payloads), 5)
        for i, p in enumerate(payloads):
            self.assertIsNotNone(p)
            self.assertEqual(p["source_url"], f"https://www.youtube.com/watch?v={playlist_raw_entries[i]['id']}")
            self.assertEqual(p["title"], playlist_raw_entries[i]["title"])
            self.assertEqual(p["duration_ms"], playlist_raw_entries[i]["duration"] * 1000)
            self.assertEqual(p["uploader"], "AI Channel")

    def test_playlist_video3_claude_tutorial_format_pairing(self):
        # Video 3 (bwPCZhNGTIw): Claude Tutorial for Beginners (Step by Step, Free Plan)
        # Format 313: 4K VP9 WebM (2.27 GB), Format 137: 1080p AVC1 MP4, Format 251: Opus WebM, Format 140: M4A
        entry_v3 = {
            "formats": [
                {
                    "format_id": "313",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=313",
                    "vcodec": "vp9",
                    "acodec": "none",
                    "ext": "webm",
                    "height": 2160,
                    "tbr": 11500,
                    "filesize": 2275349610,
                    "protocol": "https",
                },
                {
                    "format_id": "271",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=271",
                    "vcodec": "vp9",
                    "acodec": "none",
                    "ext": "webm",
                    "height": 1440,
                    "tbr": 5000,
                    "filesize": 478545400,
                    "protocol": "https",
                },
                {
                    "format_id": "137",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=137",
                    "vcodec": "avc1.640028",
                    "acodec": "none",
                    "ext": "mp4",
                    "height": 1080,
                    "tbr": 3000,
                    "filesize": 278184144,
                    "protocol": "https",
                },
                {
                    "format_id": "248",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=248",
                    "vcodec": "vp9",
                    "acodec": "none",
                    "ext": "webm",
                    "height": 1080,
                    "tbr": 1800,
                    "filesize": 156724676,
                    "protocol": "https",
                },
                {
                    "format_id": "140",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=140",
                    "vcodec": "none",
                    "acodec": "mp4a.40.2",
                    "ext": "m4a",
                    "abr": 128,
                    "filesize": 25000000,
                    "protocol": "https",
                },
                {
                    "format_id": "251",
                    "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=251",
                    "vcodec": "none",
                    "acodec": "opus",
                    "ext": "webm",
                    "abr": 130,
                    "filesize": 26000000,
                    "protocol": "https",
                },
            ]
        }

        formats = _entry_formats(entry_v3)
        self.assertTrue(len(formats) >= 4)

        # 4K WebM (313) must be paired with itag 251 (Opus WebM)
        fmt_4k = next((f for f in formats if f.get("format_id") == "313"), None)
        self.assertIsNotNone(fmt_4k)
        self.assertEqual(fmt_4k["ext"], "webm")
        self.assertEqual(fmt_4k["height"], 2160)
        self.assertIsNotNone(fmt_4k.get("merge_audio"))
        self.assertEqual(fmt_4k["merge_audio"]["format_id"], "251")
        self.assertEqual(fmt_4k["merge_audio"]["ext"], "webm")
        self.assertEqual(fmt_4k["filesize"], 2275349610 + 26000000)

        # 1080p MP4 (137) must be paired with itag 140 (M4A)
        fmt_1080p_mp4 = next((f for f in formats if f.get("format_id") == "137"), None)
        self.assertIsNotNone(fmt_1080p_mp4)
        self.assertEqual(fmt_1080p_mp4["ext"], "mp4")
        self.assertEqual(fmt_1080p_mp4["height"], 1080)
        self.assertIsNotNone(fmt_1080p_mp4.get("merge_audio"))
        self.assertEqual(fmt_1080p_mp4["merge_audio"]["format_id"], "140")
        self.assertEqual(fmt_1080p_mp4["merge_audio"]["ext"], "m4a")
        self.assertEqual(fmt_1080p_mp4["filesize"], 278184144 + 25000000)

    def test_playlist_all_five_videos_format_compatibility(self):
        # Verify that for each video format type across the playlist, video-only streams
        # are compatible with Android MediaMuxer and container rules
        test_streams = [
            ("8v0s64d1Sic", "av01", "mp4", 2160),
            ("_i-MHQDaBR4", "avc1", "mp4", 1080),
            ("bwPCZhNGTIw", "vp9", "webm", 2160),
            ("7HQ4FWVwAsk", "avc1", "mp4", 1080),
            ("2nQO0y1NvqU", "vp9", "webm", 2160),
        ]

        for vid, vcodec, ext, height in test_streams:
            self.assertTrue(_is_muxer_compatible_video(vcodec), f"Codec {vcodec} for {vid} must be muxer compatible")
            video_fmt = {
                "format_id": f"test_{vid}",
                "ext": ext,
                "url": f"https://rr1---sn-xxx.googlevideo.com/videoplayback?id={vid}",
            }
            audios = [
                {"format_id": "140", "ext": "m4a", "abr": 128, "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=140"},
                {"format_id": "251", "ext": "webm", "abr": 130, "url": "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=251"},
            ]
            pair = _pick_audio_pair(video_fmt, audios)
            self.assertIsNotNone(pair)
            if ext == "mp4":
                self.assertEqual(pair["format_id"], "140")
                self.assertEqual(pair["ext"], "m4a")
            elif ext == "webm":
                self.assertEqual(pair["format_id"], "251")
                self.assertEqual(pair["ext"], "webm")


class TestInstagramResolver(unittest.TestCase):

    def test_build_title(self):
        post1 = DummyObject(caption="Check out this cool photo!\nSecond line", owner_username="user1", shortcode="C123")
        self.assertEqual(_build_title(post1), "Check out this cool photo!")

        post2 = DummyObject(caption=None, owner_username="user2", shortcode="C456")
        self.assertEqual(_build_title(post2), "user2_C456")

    def test_payload_generators(self):
        post = DummyObject(
            caption="Sample caption",
            owner_username="john_doe",
            shortcode="ABC123xyz",
            video_url="http://example.com/video.mp4",
            url="http://example.com/image.jpg",
        )

        v_payload = _video_payload(post)
        self.assertEqual(v_payload["kind"], "video")
        self.assertEqual(v_payload["media_url"], "http://example.com/video.mp4")

        i_payload = _image_payload(post)
        self.assertEqual(i_payload["kind"], "image")
        self.assertEqual(i_payload["media_url"], "http://example.com/image.jpg")

    def test_sidecar_node_payload(self):
        post = DummyObject(caption="Carousel post", owner_username="jane", shortcode="SIDE1")
        video_node = DummyObject(is_video=True, video_url="http://example.com/sidecar.mp4")
        image_node = DummyObject(is_video=False, display_url="http://example.com/sidecar.jpg")

        v_res = _sidecar_node_payload(post, video_node)
        self.assertEqual(v_res["kind"], "video")
        self.assertEqual(v_res["media_url"], "http://example.com/sidecar.mp4")

        i_res = _sidecar_node_payload(post, image_node)
        self.assertEqual(i_res["kind"], "image")
        self.assertEqual(i_res["media_url"], "http://example.com/sidecar.jpg")


if __name__ == "__main__":
    unittest.main()
