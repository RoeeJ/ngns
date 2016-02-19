/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    This class, net.sf.odinms.client.sexbot.ChatHandler.java, was written
    by Ethan Jenkins (Sathon) and is his property.
*/


package client.sexbot

import com.beust.klaxon.*
import com.google.gson.GsonBuilder
import org.apache.commons.io.IOUtils
import tools.StringUtil
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.*
import com.google.gson.Gson
import tools.FilePrinter
import java.io.Reader

class ChatHandler {

    companion object {
        private val gson = GsonBuilder().create()

        fun processDefinition(splitted: Array<String>, name: String): String {
            if (splitted[2].toLowerCase() == "hao") {
                return "One hot piece of ass"
            }
            val word = splitted.slice(2 until splitted.size)
            try {
                val def = "http://api.wordnik.com/v4/word.json/$word/definitions?limit=1&includeRelated=true&useCanonical=false&includeTags=false&api_key=a2a73e7b926c924fad7001ca3111acd55af2ffabf50eb4ae5"
                        .httpGet()
                        .responseObject(Wordnik.Deserializer()).third
                FilePrinter.print("wordnik.txt",def.toString())
                return def.text
            } catch (e: Exception) {
                FilePrinter.printError("wordnik_e.txt",e)
                e.printStackTrace()

            }
            try {
                val definitions = "http://api.urbandictionary.com/v0/define?term=$word"
                        .httpGet()
                        .responseObject(UrbanDictionary.Deserializer()).third
                val topdef = definitions.list.sortedBy({ it.thumbsUp }).first()
                FilePrinter.print("urban.txt",topdef.toString())
                return "${topdef.definition} - ${topdef.example}"
            } catch (ex: Exception) {
                FilePrinter.printError("urban_e.txt",ex);
                ex.printStackTrace()
                //MegatronListener.getInstance().log("decepticons",StringUtil.exceptionStacktraceToString(ex));
            }

            return "idk"
        }

        fun generateJoke(s: String): String {
            var response = s + " is not funny.."
            try {
                when (s) {
                    "yomama" -> {
                        response = (Parser().parse(IOUtils.toString(URL("http://api.yomomma.info/")).byteInputStream()) as JsonObject).string("joke").orEmpty()
                    }
                    "chuck" -> {
                        response = (Parser().parse(IOUtils.toString(URL("http://api.icndb.com/jokes/random/")).byteInputStream()) as JsonObject).obj("value")?.string("joke").orEmpty()
                    }
                    "elizabeth", "elizabethian", "shakespeare", "shakespear" -> {
                        response = (Parser().parse(IOUtils.toString(URL("http://insulted.bravehost.com/?json")).byteInputStream()) as JsonObject).string("insult").orEmpty()
                    }
                }
                return response
            } catch (e: Exception) {
                return response
            }

        }
    }
    data class Wordnik(
        val word: String,
        val text: String) {
        class Deserializer : ResponseDeserializable<Wordnik> {
            override fun deserialize(reader: Reader) = Gson().fromJson(reader, Wordnik::class.java)
        }
    }
    data class UrbanDictionary(val tags: Array<String>, val list: Array<UrbanDefinition>) {
        class Deserializer: ResponseDeserializable<UrbanDictionary> {
            override fun deserialize(reader: Reader) = Gson().fromJson(reader, UrbanDictionary::class.java)
        }
    }
    data class UrbanDefinition(val word: String, val definition: String, val example: String, val thumbsUp: Int, val thumbsDown: Int) {
        class Deserializer: ResponseDeserializable<UrbanDefinition> {
            override fun deserialize(reader: Reader) = Gson().fromJson(reader, UrbanDefinition::class.java)
        }
    }
}