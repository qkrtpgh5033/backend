package com.idea5.four_cut_photos_map.domain.shop.controller;

import com.idea5.four_cut_photos_map.domain.shop.dto.response.*;
import com.idea5.four_cut_photos_map.domain.shop.service.ShopService;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.DISTANCE_IS_EMPTY;


//@RestController
@RequestMapping("/shop")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ShopService shopService;

    /**
     * todo : 카카오맵 api 완료시, 표시할 shop 조회
     * 순서)
     * 1. 맵 api로부터, 현재 위치 기준으로 "브랜드별" 조회 및 응답값 받아오기
     * 2. DB와 응답값을 비교
     * 3. 클라이언트에게 응답.
     */

    /**
     * 키워드 검색 (리스트 조회)
     * ex)
     * 하루필름, 인생네컷, 포토이즘, 포토그레이
     */

    @GetMapping(value = "/search")
    public ResponseEntity<List<ResponseShop>> keywordSearch(@RequestParam(defaultValue = "즉석사진") String keyword){
        // 1. 카카오맵 api 응답 데이터 받아오기
        KaKaoSearchResponseDto apiShopJson = shopService.searchByKeyword(keyword);

        // 2. 카카오 api 응답 DTO 에서 List<DTO.Document>로 변환
        List<KaKaoSearchResponseDto.Document> dtos = new ArrayList<>();

        for (int i = 0; i < apiShopJson.getDocuments().length; i++) {
            KaKaoSearchResponseDto.Document dto = new KaKaoSearchResponseDto.Document();

            //log.info("name="+apiShopJson.getDocuments()[i].getPlace_name());
            //log.info("distance="+apiShopJson.getDocuments()[i].getDistance());

            /** TODO:
             1차 리팩토링-> builder로 수정
             2차 리팩토링 -> 현업에서 사용하는 방식(Jackson으로 처리)으로 수정
            */

            String place_name = apiShopJson.getDocuments()[i].getPlace_name();
            String road_address_name = apiShopJson.getDocuments()[i].getRoad_address_name();
            String x = apiShopJson.getDocuments()[i].getX();
            String y = apiShopJson.getDocuments()[i].getY();
            String distance = apiShopJson.getDocuments()[i].getDistance();

            dto.setPlace_name(place_name);
            dto.setRoad_address_name(road_address_name);
            dto.setX(x);
            dto.setY(y);
            dto.setDistance(distance);

            dtos.add(dto);
        }

        // 3. db 데이터와 비교
        List<ResponseShop> shops = shopService.findShops(dtos, keyword);

        return ResponseEntity.ok(shops);
    }


    /*
    //현재 위치 기준, 반경 2km
    @GetMapping("/search/marker")
    public ResponseEntity<Map<String, List<ResponseMarker>>> currentLocationSearch(@ModelAttribute @Valid RequestShop requestShop){

        String[] names = Brand.Names; // 브랜드명 ( 하루필름, 인생네컷 ... )

        // 카카오 api -> 필요한 변수 = {브랜드명, 위도, 경도, 반경}
        // 일단 샘플로 테스트
        Map<String, List<ShopDto>> maps = new HashMap<>();
        for (String brandName : names) {
            List<ShopDto> list = TempKaKaO.tempDataBySearch(brandName);
            maps.put(brandName, list);
        }

        Map<String, List<ResponseMarker>> maker = shopService.findMaker(maps);


        return ResponseEntity.ok(maker);
    }
     */

    // todo : @Validated 유효성 검사 시, httpstatus code 전달하는 방법
    @GetMapping("/detail/{shopId}")
    public ResponseEntity<ResponseShopDetail> detail(@PathVariable(name = "shopId") Long id, @RequestParam(name = "distance", required = false, defaultValue = "") String distance) {
        if (distance.isEmpty()){
            throw new BusinessException(DISTANCE_IS_EMPTY);
        }
        ResponseShopDetail shopDetailDto = shopService.findShopById(id, distance);
        return ResponseEntity.ok(shopDetailDto);

    }
}
