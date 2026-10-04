package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.SectionCreateRequest;
import com.ballpark.ticketing.game.dto.SectionResponse;
import com.ballpark.ticketing.game.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sections")
public class AdminSectionController {

	private final SectionService sectionService;

	public AdminSectionController(SectionService sectionService) {
		this.sectionService = sectionService;
	}

	@PostMapping
	public ResponseEntity<SectionResponse> createSection(@Valid @RequestBody SectionCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(request));
	}
}
