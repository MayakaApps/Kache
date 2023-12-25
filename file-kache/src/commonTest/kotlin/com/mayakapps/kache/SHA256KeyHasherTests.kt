/*
 * Copyright 2023 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mayakapps.kache

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SHA256KeyHasherTests {

    @Test
    fun testEmptyString() = runTest {
        val hash = SHA256KeyHasher.transform("")
        assertEquals("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", hash)
    }

    @Test
    fun testStringWithSpecialCharacters() = runTest {
        val hash = SHA256KeyHasher.transform(
            "Key !@#\$%^&*)(_+\\/- مفتاح. \uD83D\uDE02\uD83E\uDEF1\uD83C\uDFFD\u200D\uD83E\uDEF2\uD83C\uDFFF",
        )

        assertEquals("AAB8B469D0D43B2C0546803CE87BA36B0559E47CA5572BCC843F3273DF0C315B", hash)
    }

    @Test
    fun testLongString() = runTest {
        val hash = SHA256KeyHasher.transform(
            "Av10GLREbz6hf7DtmvjBlT4zpusN5fvvlj054gES3soJacpUxmTIcUKwMK9MkjhtO9fvaRKy8PiCEUSXOOCjdkqk4ICwOb" +
                    "4SOyQe3vfxb6S6ZYyi39RNaysrOCIBNCEXNRtFKfPH0Xw2MpwcREXLFcu21SkRGCF9KlBRiHpfgMV94O8eyD1QY0x2cQG" +
                    "GsdOvMTSClKhin6WdUtgzapBTH83JXHGXkv40wJ2auckeYxohkM8XaHcfFRX5sjLxpAZNAD2S4Tejzbn0eX632BCpWYR7" +
                    "ET39ZxvkwUw5xLImNOwolQZcR6iCPRZFiznT2FBlK9vQuuR7B3aRrocnuHSi5FvXWNi5lWmrsai4eDUwhcrlR3QlwE8Pt" +
                    "AIxPJFjCue4P5EipeU7Bdf7CnOQTV7FNfZkDfpvOmia8mY8rYOhyxxF1Vn7uh2T1bpD33lwR674hJK6QMGXwj1qRIozy1" +
                    "yjZLMYfyWWeRIT1IwVzR9dO7Av6ErHh0kPwENosAcjXehXa9DV36zPOcNxJQAH3QWFb2Lv5fisGRa1iVPHJxcusHWCy8w" +
                    "tpWV4D76T9n2yVt9F2fslLCjULQdedcFzDkJnyi7uXvBuNqXZHCdUyypvMVUsnad17pgVCBM9XgKyDU7UWd0JesY7Kf84" +
                    "AoCQtOmz2HdWEU6SyC6Bk98gKjmh3a77mIaGCd0VJlm9JXRtclQ37ZNA5KYhLRLTxrh5auB4AOFMOKjZFTf1gNTR2i0SV" +
                    "p75NbCDBNafFVKSJHnb0wMboRpzKr4uKbNaoXbLNzhOrXpdlYOQvfOImVbY0z8rr3VzGEqUhCXsCctNtZy9X9r3pD62QO" +
                    "gH9hIzCv0L3BuTXZnC1Q29IcNxgcnYjsZicyLnqJYcCTddrpzAvjzi9MWkmDOq00iaOFJDpRgDz54tqwK6kIi0WAaMTbk" +
                    "5f7SlcnSJ2egi7kGrsO9HA9az1KX9qbHUeLRb4YMmstuZpQyclVzTZfPvrdeoPVugUdum24GOt3qvxdqA7MBMm2pskKvp",
        )

        assertEquals("275A2E01BA596AA4571E304F33EC5AB61381C926651E6B200CCD58A0EB2A8746", hash)
    }

    @Test
    fun testRandomKeys() = runTest {
        val data = listOf(
            "6rWhI9qdZ80iLQKUQNeCRZ4R40eA0Qd0SxG0DJf9RQNVqfei" to
                    "DB3D43E38ACEF90EAB976E4A1CC10DA115F96253F1E0A9798F849B8D8F0B7114",
            "AIts6azqkLsnlrWtWeT5LVJDLAwFgpslMAF7AX83Q4FRBOln" to
                    "00A53B4BC4BAE1E8AA0E7AE475AB1C960CABE8F2CC0D2DCDC82025B8B3E06A80",
            "VMEP2eOyJGvainOpsLX52BC0Pd6nrXyLUuWahP5ADxNMibtE" to
                    "DA403F07C43188D3109C79794C681DB71F300D655ACE4F9E3C230471A84A1AFA",
        )

        data.forEach { (key, hash) ->
            assertEquals(hash, SHA256KeyHasher.transform(key))
        }
    }
}
