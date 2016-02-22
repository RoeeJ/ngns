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


package client.sexbot;

import java.io.*;
import java.lang.String;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.server.Server;
import org.apache.commons.io.IOUtils;
import server.MegatronListener;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.StringUtil;

import javax.annotation.Generated;

public class ChatHandler
{
    private static Gson gson = new GsonBuilder().create();

    public static String processChat(String[] splitted, String name)
	{
		String response = "I don't know what you're talking about.";

		for(int i = 0; i < splitted.length; i++)
		{
			if(splitted[i].equals("u"))
				splitted[i] = "you";
		}

		String text = StringUtil.joinStringFrom(splitted, 1);

		text = text.replace(",", "");
		text = text.replace(".", "");
		text = text.replace("!", "");
		text = text.replace("?", "");

		Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;

        try
        {
            ps = con.prepareStatement("SELECT response FROM responses WHERE chat = ?");
            ps.setString(1, text);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            	response = rs.getString("response");
            else
            {
            	return null;//"my developer hasn't added a response for that yet.";
            }
            ps.close();
        }
    	catch (SQLException e) {System.out.println("SQL Exception: " + e);}

		return name + ", " + response;
	}

	public static String processDefinition(String[] splitted, String name)
	{
        if(splitted[2].toLowerCase().equals("hao")){
            return "One hot piece of ass";
        }
		String response = "I don't know what that word means";
		String word = StringUtil.joinStringFrom(splitted, 2);
        try {
            String js = IOUtils.toString(new URL(String.format("http://api.wordnik.com/v4/word.json/%s/definitions?limit=1&includeRelated=true&useCanonical=false&includeTags=false&api_key=a2a73e7b926c924fad7001ca3111acd55af2ffabf50eb4ae5", URLEncoder.encode(word,"UTF-8"))));
            js = js.substring(1,js.length()-1);
            js = js.replace(" ","%20");
            Definition definition = gson.fromJson(js,Definition.class);
            response = definition.getText();
            return URLDecoder.decode(response,"UTF-8");
        }
        catch (Exception e){
            try {
                String _js = IOUtils.toString(new URL(String.format("http://api.urbandictionary.com/v0/define?term=%s", URLEncoder.encode(word,"UTF-8"))));
                //_js = _js.substring(1, _js.length() - 1);
                //_js = _js.replace(" ","%20");
                List<DList> defs = gson.fromJson(_js, Urban.class).getList();
                DList def = defs.get(new Random(System.currentTimeMillis()).nextInt(defs.size()-1));
                response = String.format("%s - %s",def.getDefinition(),def.getExample());
                return URLDecoder.decode(response,"UTF-8");
            } catch (Exception ex) {
                //MegatronListener.getInstance().log("decepticons",StringUtil.exceptionStacktraceToString(ex));
            }
        }
        return "idk";
	}

	public static String generateInsult()
	{
        return "";
    }

    public static String getFromURL(String url,String key) {
        return "test";
    }

    public static String generateJoke(String s) {
        String response = s + " is not funny..";
        try {
            switch (s) {
                case "yomama": {
                    response = gson.fromJson(IOUtils.toString(new URL("http://api.yomomma.info/")), Joke.class).toString();
                    break;
                }
                case "chuck": {
                    response = gson.fromJson(IOUtils.toString(new URL("http://api.icndb.com/jokes/random/")), Chuck.class).toString();
                    break;
                }
                case "elizabeth":
                case "elizabethian":
                case "shakespeare":
                case "shakespear":
                {
                    response = gson.fromJson(IOUtils.toString(new URL("http://insulted.bravehost.com/?json")), Elizabeth.class).getInsult();
                    break;
                }
            }
            return response;
        }
        catch (Exception e){
            return response;
        }
    }

    @Generated("org.jsonschema2pojo")
    private class Joke {
        private String joke;

        // Getters and setters are not required for this example.
        // GSON sets the fields directly using reflection.

        @Override
        public String toString() {
            return joke;
        }
    }
    @Generated("org.jsonschema2pojo")
    public class Elizabeth {

        @Expose
        private String insult;

        /**
         *
         * @return
         * The insult
         */
        public String getInsult() {
            return insult;
        }

        /**
         *
         * @param insult
         * The insult
         */
        public void setInsult(String insult) {
            this.insult = insult;
        }

    }

    @Generated("org.jsonschema2pojo")
    private class Chuck {

        @Expose
        private String type;
        @Expose
        private Value value;

        /**
         *
         * @return
         * The type
         */
        public String getType() {
            return type;
        }

        /**
         *
         * @param type
         * The type
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         *
         * @return
         * The value
         */
        public Value getValue() {
            return value;
        }

        /**
         *
         * @param value
         * The value
         */
        public void setValue(Value value) {
            this.value = value;
        }
        public String toString(){
            return getValue().getJoke();
        }

    }

    @Generated("org.jsonschema2pojo")
    private class Value {

        @Expose
        private Integer id;
        @Expose
        private String joke;
        @Expose
        private List<Object> categories = new ArrayList<Object>();

        /**
         *
         * @return
         * The id
         */
        public Integer getId() {
            return id;
        }

        /**
         *
         * @param id
         * The id
         */
        public void setId(Integer id) {
            this.id = id;
        }

        /**
         *
         * @return
         * The joke
         */
        public String getJoke() {
            return joke;
        }

        /**
         *
         * @param joke
         * The joke
         */
        public void setJoke(String joke) {
            this.joke = joke;
        }

        /**
         *
         * @return
         * The categories
         */
        public List<Object> getCategories() {
            return categories;
        }

        /**
         *
         * @param categories
         * The categories
         */
        public void setCategories(List<Object> categories) {
            this.categories = categories;
        }

    }

    @Generated("org.jsonschema2pojo")
    private class Definition {

        @Expose
        private List<Object> textProns = new ArrayList<Object>();
        @Expose
        private String sourceDictionary;
        @Expose
        private List<Object> exampleUses = new ArrayList<Object>();
        @Expose
        private List<Object> relatedWords = new ArrayList<Object>();
        @Expose
        private List<Object> labels = new ArrayList<Object>();
        @Expose
        private List<Object> citations = new ArrayList<Object>();
        @Expose
        private String word;
        @Expose
        private String partOfSpeech;
        @Expose
        private String sequence;
        @Expose
        private String attributionText;
        @Expose
        private String text;
        @Expose
        private Double score;

        /**
         *
         * @return
         * The textProns
         */
        public List<Object> getTextProns() {
            return textProns;
        }

        /**
         *
         * @param textProns
         * The textProns
         */
        public void setTextProns(List<Object> textProns) {
            this.textProns = textProns;
        }

        /**
         *
         * @return
         * The sourceDictionary
         */
        public String getSourceDictionary() {
            return sourceDictionary;
        }

        /**
         *
         * @param sourceDictionary
         * The sourceDictionary
         */
        public void setSourceDictionary(String sourceDictionary) {
            this.sourceDictionary = sourceDictionary;
        }

        /**
         *
         * @return
         * The exampleUses
         */
        public List<Object> getExampleUses() {
            return exampleUses;
        }

        /**
         *
         * @param exampleUses
         * The exampleUses
         */
        public void setExampleUses(List<Object> exampleUses) {
            this.exampleUses = exampleUses;
        }

        /**
         *
         * @return
         * The relatedWords
         */
        public List<Object> getRelatedWords() {
            return relatedWords;
        }

        /**
         *
         * @param relatedWords
         * The relatedWords
         */
        public void setRelatedWords(List<Object> relatedWords) {
            this.relatedWords = relatedWords;
        }

        /**
         *
         * @return
         * The labels
         */
        public List<Object> getLabels() {
            return labels;
        }

        /**
         *
         * @param labels
         * The labels
         */
        public void setLabels(List<Object> labels) {
            this.labels = labels;
        }

        /**
         *
         * @return
         * The citations
         */
        public List<Object> getCitations() {
            return citations;
        }

        /**
         *
         * @param citations
         * The citations
         */
        public void setCitations(List<Object> citations) {
            this.citations = citations;
        }

        /**
         *
         * @return
         * The word
         */
        public String getWord() {
            return word;
        }

        /**
         *
         * @param word
         * The word
         */
        public void setWord(String word) {
            this.word = word;
        }

        /**
         *
         * @return
         * The partOfSpeech
         */
        public String getPartOfSpeech() {
            return partOfSpeech;
        }

        /**
         *
         * @param partOfSpeech
         * The partOfSpeech
         */
        public void setPartOfSpeech(String partOfSpeech) {
            this.partOfSpeech = partOfSpeech;
        }

        /**
         *
         * @return
         * The sequence
         */
        public String getSequence() {
            return sequence;
        }

        /**
         *
         * @param sequence
         * The sequence
         */
        public void setSequence(String sequence) {
            this.sequence = sequence;
        }

        /**
         *
         * @return
         * The attributionText
         */
        public String getAttributionText() {
            return attributionText;
        }

        /**
         *
         * @param attributionText
         * The attributionText
         */
        public void setAttributionText(String attributionText) {
            this.attributionText = attributionText;
        }

        /**
         *
         * @return
         * The text
         */
        public String getText() {
            return text;
        }

        /**
         *
         * @param text
         * The text
         */
        public void setText(String text) {
            this.text = text;
        }

        /**
         *
         * @return
         * The score
         */
        public Double getScore() {
            return score;
        }

        /**
         *
         * @param score
         * The score
         */
        public void setScore(Double score) {
            this.score = score;
        }

    }

    @Generated("org.jsonschema2pojo")
    public class DList {

        @Expose
        private Integer defid;
        @Expose
        private String word;
        @Expose
        private String author;
        @Expose
        private String permalink;
        @Expose
        private String definition;
        @Expose
        private String example;
        @SerializedName("thumbs_up")
        @Expose
        private Integer thumbsUp;
        @SerializedName("thumbs_down")
        @Expose
        private Integer thumbsDown;
        @SerializedName("current_vote")
        @Expose
        private String currentVote;

        /**
         *
         * @return
         * The defid
         */
        public Integer getDefid() {
            return defid;
        }

        /**
         *
         * @param defid
         * The defid
         */
        public void setDefid(Integer defid) {
            this.defid = defid;
        }

        /**
         *
         * @return
         * The word
         */
        public String getWord() {
            return word;
        }

        /**
         *
         * @param word
         * The word
         */
        public void setWord(String word) {
            this.word = word;
        }

        /**
         *
         * @return
         * The author
         */
        public String getAuthor() {
            return author;
        }

        /**
         *
         * @param author
         * The author
         */
        public void setAuthor(String author) {
            this.author = author;
        }

        /**
         *
         * @return
         * The permalink
         */
        public String getPermalink() {
            return permalink;
        }

        /**
         *
         * @param permalink
         * The permalink
         */
        public void setPermalink(String permalink) {
            this.permalink = permalink;
        }

        /**
         *
         * @return
         * The definition
         */
        public String getDefinition() {
            return definition;
        }

        /**
         *
         * @param definition
         * The definition
         */
        public void setDefinition(String definition) {
            this.definition = definition;
        }

        /**
         *
         * @return
         * The example
         */
        public String getExample() {
            return example;
        }

        /**
         *
         * @param example
         * The example
         */
        public void setExample(String example) {
            this.example = example;
        }

        /**
         *
         * @return
         * The thumbsUp
         */
        public Integer getThumbsUp() {
            return thumbsUp;
        }

        /**
         *
         * @param thumbsUp
         * The thumbs_up
         */
        public void setThumbsUp(Integer thumbsUp) {
            this.thumbsUp = thumbsUp;
        }

        /**
         *
         * @return
         * The thumbsDown
         */
        public Integer getThumbsDown() {
            return thumbsDown;
        }

        /**
         *
         * @param thumbsDown
         * The thumbs_down
         */
        public void setThumbsDown(Integer thumbsDown) {
            this.thumbsDown = thumbsDown;
        }

        /**
         *
         * @return
         * The currentVote
         */
        public String getCurrentVote() {
            return currentVote;
        }

        /**
         *
         * @param currentVote
         * The current_vote
         */
        public void setCurrentVote(String currentVote) {
            this.currentVote = currentVote;
        }

    }
    @Generated("org.jsonschema2pojo")
    public class Urban {

        @Expose
        private java.util.List<String> tags = new ArrayList<String>();
        @SerializedName("result_type")
        @Expose
        private String resultType;
        @Expose
        private java.util.List<DList> list = new ArrayList<DList>();
        @Expose
        private java.util.List<String> sounds = new ArrayList<String>();

        /**
         *
         * @return
         * The tags
         */
        public java.util.List<String> getTags() {
            return tags;
        }

        /**
         *
         * @param tags
         * The tags
         */
        public void setTags(java.util.List<String> tags) {
            this.tags = tags;
        }

        /**
         *
         * @return
         * The resultType
         */
        public String getResultType() {
            return resultType;
        }

        /**
         *
         * @param resultType
         * The result_type
         */
        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        /**
         *
         * @return
         * The list
         */
        public java.util.List<DList> getList() {
            return list;
        }

        /**
         *
         * @param list
         * The list
         */
        public void setList(java.util.List<DList> list) {
            this.list = list;
        }

        /**
         *
         * @return
         * The sounds
         */
        public java.util.List<String> getSounds() {
            return sounds;
        }

        /**
         *
         * @param sounds
         * The sounds
         */
        public void setSounds(java.util.List<String> sounds) {
            this.sounds = sounds;
        }

    }
}