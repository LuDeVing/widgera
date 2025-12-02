package com.widgera.repository;

import com.widgera.entity.User;
import com.widgera.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserImageRepository extends JpaRepository<UserImage, Long> {

    Optional<UserImage> findByUserAndImageHash(User user, String imageHash);

    Optional<UserImage> findByIdAndUser(Long id, User user);

    List<UserImage> findByUser(User user);

}
