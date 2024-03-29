package com.groupproject.boomerang.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupproject.boomerang.Constants;
import com.groupproject.boomerang.model.ResponseBody.GoogleTextSearchAPIResponse.GoogleTextSearchResponse;
import com.groupproject.boomerang.model.ResponseBody.GoogleTextSearchAPIResponse.TextSearchResult;
import com.groupproject.boomerang.model.ResponseBody.HotelResponse.hotelResponseBody;
import com.groupproject.boomerang.model.ResponseBody.YelpTextSearchAPIResponse.YelpTextSearchResult;
import com.groupproject.boomerang.model.entity.Hotel;
import com.groupproject.boomerang.model.entity.TouristAttractions;
import com.groupproject.boomerang.utils.BoomerangException;
import com.groupproject.boomerang.utils.HTTPRequest;
import com.groupproject.boomerang.model.ResponseBody.HotelResponse.hotelResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service
public class HotelService {



    @Autowired
    HTTPRequest requestHelper;

    @Autowired
    Constants constants;

    // HttpRequest Parser Helper
    private static final String GET_RECOMMENDED_HOTEL = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s";
    private final String PAGE_TOKEN_QUERY = "&pagetoken=";

    // public Responsebody , String next_page_token
    public hotelResponse getHotel (String type, String[] option, int partySize, int withKid, int withPet, int zipcode, String next_page_token, String limit) throws UnsupportedEncodingException {

        Map<String, List<String>> TOURIST_ATTRACTION_DATA_MAPPING = constants.TOURIST_ATTRACTION_DATA_MAPPING;
        hotelResponse hotelResponse = new hotelResponse();

        StringBuilder query = new StringBuilder();

        type  = encode(type);

        query.append(type).append("+"); // "+" == " ";
        if(option!=null || option.length!=0)
        {
            StringBuilder sb = new StringBuilder();
            for(String str : option)
            {
                sb.append(str).append(" ");//"+"
            }

            query.append(sb);
        }
        if(withKid==1) query.append(TOURIST_ATTRACTION_DATA_MAPPING.get("kids").get(0)).append("+");
        if(withPet==1) query.append(TOURIST_ATTRACTION_DATA_MAPPING.get("pets").get(0)).append("+");

        query.append(zipcode);
        String queryStr = query.toString();

        System.out.println(queryStr);
        //String url = String.format(GET_RECOMMENDED_HOTEL,"" + partySize, "" + zipcode, "" + withPet, "" + withKid, departureDate.toString(), returnDate.toString(), type, price, style, area, city, Constants.GOOGLE_MAP_API_KEY);
        String url = String.format(GET_RECOMMENDED_HOTEL,type, Constants.GOOGLE_MAP_API_KEY);
        int limitCount =Integer.parseInt(limit); // 20;

        if (next_page_token!=null)
        {
            next_page_token = encode(next_page_token);
            url += PAGE_TOKEN_QUERY+next_page_token;
        }

        GoogleTextSearchResponse textSearchResponse = requestHelper.makeRequest(GoogleTextSearchResponse.class, url, new GoogleTextSearchResponse());





        if(textSearchResponse == null)
        {
            System.out.println("The response is null");
            hotelResponse.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(); // 就是 500
            //throw new BoomerangException("Invalud Google API Response !");
            return hotelResponse; // 把 500 返回给前端
        }

        hotelResponseBody body = new hotelResponseBody();//touristAttractionsResponseBody
        body.nextPageToken = textSearchResponse.nextPageToken;

        TextSearchResult[] results  = textSearchResponse.results;//只是 google api 返回结果的引用

        limitCount = Math.min(limitCount, results.length);
        Hotel[] frontEndResults = new Hotel[limitCount];

        /** {照twitch抄}**/

        // 写个 pq 排序
        for(int i = 0; i < limitCount; i++)
        {
            TextSearchResult result = results[i];
            Hotel eachFrontEndNeeds = new Hotel();

            eachFrontEndNeeds.business_status = result.businessStatus;
            eachFrontEndNeeds.formatted_address = result.formattedAddress;
            //!!!
            eachFrontEndNeeds.location = result.geometry.location;
            eachFrontEndNeeds.name = result.name;
            eachFrontEndNeeds.place_id = result.placeID;
            eachFrontEndNeeds.rating = result.rating;
            eachFrontEndNeeds.user_ratings_total = result.userRatingsTotal;

            // 图片只拿一张
            if (result.photos!=null && result.photos.length > 0){
                eachFrontEndNeeds.photo_reference = result.photos[0].photoReference;
            }

            frontEndResults[i] = eachFrontEndNeeds;
        }

        body.results = frontEndResults;
        hotelResponse.responsebody = body;
        hotelResponse.statusCode = HttpStatus.OK.value();

        return hotelResponse;


    }


    private String encode(String param) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(param, "UTF-8");
    }

//    private TextSearchResult[] getHotelsArray(String data) throws BoomerangException {
//        try {
//            return mapper.readValue(data, YelpTextSearchResult[].class); // Arrays.aslist
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            throw new BoomerangException("Failed to parse game data from Yelp API");
//        }
//    }
}
