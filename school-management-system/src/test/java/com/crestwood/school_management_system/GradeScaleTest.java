package com.crestwood.school_management_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.crestwood.school_management_system.exception.BadRequestException;

class GradeScaleTest {
	@Test
	void derivesEveryExactGradeBoundary() {
		assertEquals("A", GradeScale.grade(75));
		assertEquals("B", GradeScale.grade(74));
		assertEquals("B", GradeScale.grade(60));
		assertEquals("C", GradeScale.grade(59));
		assertEquals("C", GradeScale.grade(50));
		assertEquals("D", GradeScale.grade(49));
		assertEquals("D", GradeScale.grade(45));
		assertEquals("E", GradeScale.grade(44));
		assertEquals("E", GradeScale.grade(40));
		assertEquals("F", GradeScale.grade(39));
	}

	@Test
	void sumsServerSideAndRejectsOutOfRangeScores() {
		assertEquals(81, GradeScale.total(16, 17, 48));
		assertThrows(BadRequestException.class, () -> GradeScale.total(21, 0, 0));
		assertThrows(BadRequestException.class, () -> GradeScale.total(0, 0, 61));
	}
}
