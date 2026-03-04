package com.kotlin.assets.repository

import com.kotlin.assets.entity.DpiEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DpiRepository : JpaRepository<DpiEntity, Long>