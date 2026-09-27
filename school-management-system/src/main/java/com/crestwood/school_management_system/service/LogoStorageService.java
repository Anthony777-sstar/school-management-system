package com.crestwood.school_management_system.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.exception.ServiceUnavailableException;

@Service
public class LogoStorageService {
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
	private final Path root;
	private final long maxFileSizeBytes;

	public LogoStorageService(@Value("${app.uploads.logo-directory}") String directory, @Value("${app.uploads.logo-max-file-size-bytes}") long maxFileSizeBytes) {
		this.root = Path.of(directory).toAbsolutePath().normalize();
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public String store(MultipartFile file) {
		validate(file);
		String extension = extension(safeFilename(file.getOriginalFilename()));
		String storedFilename = UUID.randomUUID() + extension;
		Path destination = root.resolve(storedFilename).normalize();
		if (!destination.startsWith(root)) {
			throw new BadRequestException("Invalid logo filename");
		}
		try {
			Files.createDirectories(root);
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			return "/api/v1/settings/school-profile/logo/" + storedFilename;
		} catch (IOException exception) {
			throw new ServiceUnavailableException("Logo storage is unavailable");
		}
	}

	public Resource resource(String storedFilename) {
		if (storedFilename == null || storedFilename.isBlank()) {
			throw new NotFoundException("School logo was not found");
		}
		Path path = root.resolve(storedFilename).normalize();
		if (!path.startsWith(root) || !Files.isRegularFile(path)) {
			throw new NotFoundException("School logo was not found");
		}
		return new FileSystemResource(path);
	}

	public String storedFilename(String logoUrl) {
		if (logoUrl == null || !logoUrl.startsWith("/api/v1/settings/school-profile/logo/")) {
			return null;
		}
		return logoUrl.substring(logoUrl.lastIndexOf('/') + 1);
	}

	public void delete(String storedFilename) {
		if (storedFilename == null || storedFilename.isBlank()) {
			return;
		}
		Path path = root.resolve(storedFilename).normalize();
		if (!path.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("A logo file is required");
		}
		if (file.getSize() > maxFileSizeBytes) {
			throw new BadRequestException("Logo file exceeds the configured size limit");
		}
		if (!ALLOWED_EXTENSIONS.contains(extension(safeFilename(file.getOriginalFilename())))) {
			throw new BadRequestException("Logo must be a PNG, JPEG, GIF or WebP image");
		}
	}

	private String safeFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return "logo";
		}
		String normalized = filename.replace('\\', '/').replaceAll("[\\p{Cntrl}]", "_");
		return Path.of(normalized).getFileName().toString();
	}

	private String extension(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
	}
}
