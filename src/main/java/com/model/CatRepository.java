package com.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CatRepository extends CrudRepository<Cat, Long> {

    List<Cat> findByUserChatId (Long userChatId);

}
