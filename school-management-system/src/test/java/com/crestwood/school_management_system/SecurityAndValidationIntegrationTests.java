package com.crestwood.school_management_system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAndValidationIntegrationTests {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private SchoolClassRepository classRepository;

	@Test
	void exposesOnlyRequiredAuthenticationAndEnrollmentFlowsPublicly() throws Exception {
		mockMvc.perform(get("/api/v1/students"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Authentication is required"));
		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"missing@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If that email exists in our system, a reset link was sent"));
		mockMvc.perform(post("/api/v1/enrollment-applications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "parentFullName":"Test Parent",
								  "parentEmail":"test-parent@example.com",
								  "parentPhone":"+2348000000000",
								  "studentFullName":"Test Student",
								  "studentDateOfBirth":"2014-01-10",
								  "applyingForClass":"JSS 3"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.applyingForClassName").value("JSS 3"));
		mockMvc.perform(post("/api/v1/enrollment-applications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").isNotEmpty());
	}

	@Test
	void enforcesFiveRoleBoundaries() throws Exception {
		String adminToken = login("admin@crestwoodacademy.ng", "SUPER_ADMIN");
		String studentToken = login("tobi.adeyemi@student.crestwoodacademy.ng", "STUDENT");
		String teacherToken = login("funmi.adebayo@crestwoodacademy.ng", "TEACHER");

		mockMvc.perform(get("/api/v1/dashboard/admin").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalStudents").value(2));
		mockMvc.perform(get("/api/v1/dashboard/admin").header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").isNotEmpty());
		mockMvc.perform(get("/api/v1/teachers/me/roster").param("classId", classId("JSS 3").toString()).header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").isNotEmpty());
	}

	@Test
	void preventsParentsFromReadingUnlinkedChildren() throws Exception {
		String parentToken = login("folake.adeyemi@crestwoodacademy.ng", "PARENT");
		Long halimaId = studentRepository.findByUserEmailIgnoreCase("halima.suleiman@student.crestwoodacademy.ng").orElseThrow().getId();
		mockMvc.perform(get("/api/v1/results").param("studentId", halimaId.toString()).header("Authorization", "Bearer " + parentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("This student is not linked to your account"));
	}

	@Test
	void rejectsRoleSpoofingAtLogin() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@crestwoodacademy.ng\",\"password\":\"test-only-seed-password\",\"role\":\"TEACHER\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid email or password"));
	}

	private String login(String email, String role) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"test-only-seed-password\",\"role\":\"" + role + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.get("token").asString();
	}

	private Long classId(String name) {
		return classRepository.findByName(name).orElseThrow().getId();
	}
}
