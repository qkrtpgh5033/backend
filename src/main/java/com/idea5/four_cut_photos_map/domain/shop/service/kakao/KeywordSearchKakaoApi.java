package com.idea5.four_cut_photos_map.domain.shop.service.kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestBrandSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestKeywordSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoKeywordResponseDto;
import com.idea5.four_cut_photos_map.global.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class KeywordSearchKakaoApi {

    @Value("${REST_API_KEY}")
    private String kakao_apikey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<KakaoKeywordResponseDto> searchByKeyword(RequestKeywordSearch requestKeywordSearch) throws JsonProcessingException {
        // 1. 결과값 담을 객체 생성
        List<KakaoKeywordResponseDto> resultList = new ArrayList<>();

        // 2. header 설정을 위해 HttpHeader 클래스 생성 후 HttpEntity 객체에 넣어준다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakao_apikey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. 파라미터를 사용하여 요청 URL 정의
        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + requestKeywordSearch.getKeyword()+ " 즉석사진"
                + "&x=" + requestKeywordSearch.getLongitude()
                + "&y=" + requestKeywordSearch.getLatitude();

        // 4. exchange 메서드로 api 호출
        String body = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class).getBody();

        // 5. JSON -> KakaoKeywordResponseDto로 역직렬화
        JsonNode node = objectMapper.readTree(body);
        List<String> countList = node.get("documents").findValuesAsText("place_name");

        for(int i=0; i<countList.size(); i++) {
            JsonNode documents = node.get("documents").get(i);

            KakaoKeywordResponseDto dto = KakaoKeywordResponseDto.builder()
                    .placeName(documents.get("place_name").textValue())
                    .roadAddressName(documents.get("road_address_name").textValue())
                    .longitude(documents.get("x").textValue())
                    .latitude(documents.get("y").textValue())
                    .distance(Util.distanceFormatting(documents.get("distance").textValue()))
                    .build();

            resultList.add(dto);
        }

        return resultList;
    }


    public List<KakaoResponseDto> searchByBrand(RequestBrandSearch request, int page){
        // 2. header 설정을 위해 HttpHeader 클래스 생성 후 HttpEntity 객체에 넣어준다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakao_apikey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. 파라미터를 사용하여 요청 URL 정의
        // ex) https://dapi.kakao.com/v2/local/search/keyword?query=${}&x=${}&y=${}&sort=distance
        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + request.getBrand()
                + "&x="+request.getLongitude()
                + "&y="+request.getLatitude()
                + "&size=15"
                + "&page="+page
                + "&sort=distance"; // 거리순
        System.out.println("apiURL = " + apiURL);

        // 4. exchange 메서드로 api 호출
        String body = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class).getBody();
        List<KakaoResponseDto> list = Util.jackson2(body, request.getBrand());
        return list;
    }


    public List<KakaoResponseDto> searchMarkers(RequestShop shop, String brandName) {
        // 2. header 설정을 위해 HttpHeader 클래스 생성 후 HttpEntity 객체에 넣어준다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakao_apikey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. 파라미터를 사용하여 요청 URL 정의
        // ex) https://dapi.kakao.com/v2/local/search/keyword?query=${}&x=${}&y=${}&sort=distance
        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + brandName
                + "&x="+shop.getLongitude()
                + "&y="+shop.getLatitude()
                + "&sort=distance" // 거리순
                + "&radius=2000"; // 반경 2km이내
        System.out.println("apiURL = " + apiURL);

        String body = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class).getBody();
        List<KakaoResponseDto> kakaoResponseDtos = Util.jackson2(body, brandName);
        return kakaoResponseDtos;
    }
}