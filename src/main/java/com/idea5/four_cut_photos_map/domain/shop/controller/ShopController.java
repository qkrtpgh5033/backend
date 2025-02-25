package com.idea5.four_cut_photos_map.domain.shop.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.idea5.four_cut_photos_map.domain.favorite.entity.Favorite;
import com.idea5.four_cut_photos_map.domain.favorite.service.FavoriteService;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoKeywordResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.ShopDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestBrandSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestKeywordSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopBrand;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopDetail;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopMarker;
import com.idea5.four_cut_photos_map.domain.shop.service.ShopService;
import com.idea5.four_cut_photos_map.domain.shoptitlelog.service.ShopTitleLogService;
import com.idea5.four_cut_photos_map.global.common.data.Brand;
import com.idea5.four_cut_photos_map.global.common.response.RsData;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import com.idea5.four_cut_photos_map.security.jwt.dto.MemberContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.BRAND_NOT_FOUND;
import static com.idea5.four_cut_photos_map.global.error.ErrorCode.DISTANCE_IS_EMPTY;


@RequestMapping("/shops")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ShopService shopService;
    private final FavoriteService favoriteService;

    private final ShopTitleLogService shopTitleLogService;


    @GetMapping(value = "")
    public ResponseEntity<RsData<List<ResponseShop>>> showListSearchedByKeyword(@ModelAttribute @Valid RequestKeywordSearch requestKeywordSearch) throws JsonProcessingException {
        // 1. 카카오맵 api 응답 데이터 받아오기
        List<KakaoKeywordResponseDto> apiShopJson = shopService.searchByKeyword(requestKeywordSearch);

        // 2. db 데이터와 비교
        List<ResponseShop> shops = shopService.findShops(apiShopJson);

        return ResponseEntity.ok(
                new RsData<List<ResponseShop>>(true, "키워드로 Shop 조회 성공", shops)
        );
    }

    @GetMapping("/brand")
    public ResponseEntity<RsData<List<ResponseShopBrand>>> showBrandListBySearch(@ModelAttribute @Valid RequestBrandSearch requestBrandSearch) {
        // api 검색전, DB에서 먼저 있는지 확인하는게 더 효율적
        List<ShopDto> shopDtos = shopService.findByBrand(requestBrandSearch.getBrand());
        if (shopDtos.isEmpty())
            throw new BusinessException(BRAND_NOT_FOUND);

        List<KakaoResponseDto> kakaoApiResponse = shopService.searchBrand(requestBrandSearch);
        List<ResponseShopBrand> resultShops = new ArrayList<>(); // 응답값 리스트

        // 카카오 맵 api로 부터 받아온 Shop 리스트와 db에 저장된 Shop 비교
        for (KakaoResponseDto apiShop : kakaoApiResponse) {
            for (ShopDto shopDto : shopDtos) {
                if (apiShop.getRoadAddressName().equals(shopDto.getRoadAddressName())) {
                    resultShops.add(ResponseShopBrand.of(apiShop));
                    break;
                }
            }
        }

        // 검색 결과, 근처에 원하는 브랜드가 없을 때
        if (resultShops.isEmpty()) {
            return ResponseEntity.ok(new RsData<>(
                    true, String.format("근처에 %s이(가) 없습니다.", requestBrandSearch.getBrand()), resultShops
            ));
        }

        return ResponseEntity.ok(new RsData<>(
                true, "brand 검색 성공", resultShops
        ));
    }


    //현재 위치 기준, 반경 2km
    @GetMapping("/marker")
    public ResponseEntity<RsData<Map<String, List<ResponseShopMarker>>>> currentLocationSearch(@ModelAttribute @Valid RequestShop requestShop) {

        String[] names = Brand.Names; // 브랜드명 ( 하루필름, 인생네컷 ... )

        Map<String, List<ResponseShopMarker>> maps = new HashMap<>();
        for (String brandName : names) {
            List<ResponseShopMarker> list = shopService.searchMarkers(requestShop, brandName);
            maps.put(brandName, list);
        }

        return ResponseEntity.ok(
                new RsData<Map<String, List<ResponseShopMarker>>>(true, "Shop 마커 성공", maps)
        );
    }

    // todo : @Validated 유효성 검사 시, httpstatus code 전달하는 방법
    @GetMapping("/{shopId}")
    public ResponseEntity<ResponseShopDetail> detail(@PathVariable(name = "shopId") Long id,
                                                     @RequestParam(name = "distance", required = false, defaultValue = "") String distance,
                                                     @AuthenticationPrincipal MemberContext memberContext) {
        if (distance.isEmpty()) {
            throw new BusinessException(DISTANCE_IS_EMPTY);
        }
        ResponseShopDetail shopDetailDto = shopService.findShopById(id, distance);

        if (memberContext != null) {
            Favorite favorite = favoriteService.findByShopIdAndMemberId(shopDetailDto.getId(), memberContext.getId());

            if (favorite == null) {
                shopDetailDto.setCanBeAddedToFavorites(true);
            } else {
                shopDetailDto.setCanBeAddedToFavorites(false);
            }
        }

        if (shopTitleLogService.existShopTitles(id)) {
            List<String> shopTitles = shopTitleLogService.getShopTitles(id);
            shopDetailDto.setShopTitles(shopTitles);
        }

        return ResponseEntity.ok(shopDetailDto);
    }
}