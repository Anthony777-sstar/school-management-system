package com.crestwood.school_management_system.service;

import com.crestwood.school_management_system.exception.BadRequestException;

public final class GradeScale {
	private GradeScale() {
	}

	public static int total(int ca1, int ca2, int exam) {
		validate(ca1, ca2, exam);
		return ca1 + ca2 + exam;
	}

	public static String grade(int total) {
		if (total < 0 || total > 100) {
			throw new BadRequestException("Result total must be between 0 and 100");
		}
		if (total >= 75) {
			return "A";
		}
		if (total >= 60) {
			return "B";
		}
		if (total >= 50) {
			return "C";
		}
		if (total >= 45) {
			return "D";
		}
		if (total >= 40) {
			return "E";
		}
		return "F";
	}

	public static void validate(int ca1, int ca2, int exam) {
		if (ca1 < 0 || ca1 > 20 || ca2 < 0 || ca2 > 20 || exam < 0 || exam > 60) {
			throw new BadRequestException("CA1 and CA2 must be between 0 and 20 and exam must be between 0 and 60");
		}
	}
}
