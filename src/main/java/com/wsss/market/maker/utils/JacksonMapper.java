package com.wsss.market.maker.utils;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.text.SimpleDateFormat;

/**
 * @author jhe
 * 
 */
public class JacksonMapper {

	private static final ObjectMapper mapper = new ObjectMapper();
	
	static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)// 字段和值都加引号  
        	.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true)//数字加引号
            .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true)//数字加引号
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }
	
	private JacksonMapper() {
	}

	public static ObjectMapper getInstance() {
		return mapper;
	}

}