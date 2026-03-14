package com.company.grc.service;

import com.company.grc.dto.UserDto;
import com.company.grc.entity.UserEntity;
import com.company.grc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // In a real app we would use Spring Security and BCrypt,
    // but requested simple implementation without heavyweight frameworks.

    public UserDto.UserResponse login(String identifier, String password) {
        UserEntity user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found with email or mobile: " + identifier));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive. Please contact administrator.");
        }

        return mapToResponse(user);
    }

    @Transactional
    public UserDto.UserResponse createUser(UserDto.CreateUserRequest request, String creatorRole) {
        if (!"SUPER_ADMIN".equals(creatorRole)) {
            throw new RuntimeException("Only SUPER_ADMIN can create new users");
        }

        if ((request.getEmail() == null || request.getEmail().isBlank()) && 
            (request.getMobileNo() == null || request.getMobileNo().isBlank())) {
            throw new RuntimeException("At least Email or Mobile Number must be provided");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getMobileNo() != null && !request.getMobileNo().isBlank() && userRepository.findByMobileNo(request.getMobileNo()).isPresent()) {
            throw new RuntimeException("Mobile number already exists");
        }

        String assignedRole = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole().toUpperCase() : "USER";

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobileNo(request.getMobileNo())
                .password(request.getPassword())
                .role(assignedRole)
                .build();

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(currentPassword)) {
            throw new RuntimeException("Current password does not match");
        }

        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Transactional
    public UserDto.UserResponse updateUser(Long id, UserDto.UpdateUserRequest request, String creatorRole) {
        if (!"SUPER_ADMIN".equals(creatorRole)) {
            throw new RuntimeException("Only SUPER_ADMIN can update users");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getMobileNo() != null) user.setMobileNo(request.getMobileNo());
        if (request.getPassword() != null) user.setPassword(request.getPassword());
        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        
        // Super Admin must always be active.
        if ("SUPER_ADMIN".equals(user.getRole())) {
            user.setActive(true);
        } else if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Long id, String creatorRole) {
        if (!"SUPER_ADMIN".equals(creatorRole)) {
            throw new RuntimeException("Only SUPER_ADMIN can delete users");
        }

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }

        userRepository.deleteById(id);
    }

    public UserDto.UserResponse getById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    public List<UserDto.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper method to look up by either email or mobile (since 10 digit constraint)
    private Optional<UserEntity> findUserByIdentifier(String identifier) {
        if (identifier.matches("\\d{10}")) {
            return userRepository.findByMobileNo(identifier);
        }
        return userRepository.findByEmail(identifier);
    }

    private UserDto.UserResponse mapToResponse(UserEntity entity) {
        return UserDto.UserResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .mobileNo(entity.getMobileNo())
                .role(entity.getRole())
                .password(entity.getPassword())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
