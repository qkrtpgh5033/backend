package com.idea5.four_cut_photos_map.review.repository;

import com.idea5.four_cut_photos_map.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByShopId(Long shopId);

}
