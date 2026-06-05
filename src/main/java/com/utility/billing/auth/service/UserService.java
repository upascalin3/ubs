package com.utility.billing.auth.service;

import com.utility.billing.auth.dto.AssignRolesRequest;
import com.utility.billing.auth.dto.CreateUserRequest;
import com.utility.billing.auth.dto.UpdateUserRequest;
import com.utility.billing.auth.dto.UserResponse;
import com.utility.billing.auth.entity.Role;
import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.mapper.UserMapper;
import com.utility.billing.auth.repository.RoleRepository;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.common.security.RoleName;
import com.utility.billing.common.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		if (request.getRoles() == null || request.getRoles().size() != 1) {
			throw new BusinessException("Exactly one role must be assigned per user");
		}
		if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
			throw new BusinessException("Email already exists");
		}
		if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
			throw new BusinessException("Phone number already exists");
		}
		Set<Role> roles = resolveRoles(request.getRoles());
		boolean staffAccount = roles.stream()
				.anyMatch(r -> RoleName.OPERATOR.equals(r.getRoleName())
						|| RoleName.FINANCE.equals(r.getRoleName()));
		User user = User.builder()
				.fullName(request.getFullName())
				.email(request.getEmail().toLowerCase())
				.phoneNumber(request.getPhoneNumber())
				.password(passwordEncoder.encode(request.getPassword()))
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.mustChangePassword(staffAccount)
				.roles(roles)
				.build();
		user = userRepository.save(user);
		log.info("Admin created user: {}", user.getEmail());
		return UserMapper.toResponse(user);
	}

	@Transactional
	public UserResponse update(UUID id, UpdateUserRequest request) {
		User user = findUser(id);
		if (request.getFullName() != null) {
			user.setFullName(request.getFullName());
		}
		if (request.getPhoneNumber() != null) {
			if (userRepository.existsByPhoneNumber(request.getPhoneNumber())
					&& !request.getPhoneNumber().equals(user.getPhoneNumber())) {
				throw new BusinessException("Phone number already exists");
			}
			user.setPhoneNumber(request.getPhoneNumber());
		}
		if (request.getStatus() != null) {
			user.setStatus(UserStatus.valueOf(request.getStatus()));
		}
		if (request.getAddress() != null) {
			user.setAddress(request.getAddress());
		}
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse activate(UUID id) {
		User user = findUser(id);
		user.setStatus(UserStatus.ACTIVE);
		user.setEmailVerified(true);
		log.info("User activated: {}", user.getEmail());
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse deactivate(UUID id) {
		User user = findUser(id);
		user.setStatus(UserStatus.INACTIVE);
		log.info("User deactivated: {}", user.getEmail());
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
		if (request.getRoles() == null || request.getRoles().size() != 1) {
			throw new BusinessException("Exactly one role must be assigned per user");
		}
		User user = findUser(id);
		user.setRoles(resolveRoles(request.getRoles()));
		log.info("Roles assigned to user {}: {}", user.getEmail(), request.getRoles());
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public void delete(UUID id) {
		UUID currentUserId = SecurityUtils.getCurrentUserId();
		if (currentUserId != null && currentUserId.equals(id)) {
			throw new BusinessException("You cannot delete your own account");
		}
		User user = findUser(id);
		userRepository.delete(user);
		log.info("User deleted: {}", user.getEmail());
	}

	public Page<UserResponse> list(Pageable pageable) {
		return userRepository.findAll(pageable).map(UserMapper::toResponse);
	}

	public Page<UserResponse> search(String keyword, Pageable pageable) {
		return userRepository.search(keyword, pageable).map(UserMapper::toResponse);
	}

	public Page<UserResponse> listCustomers(Pageable pageable) {
		return userRepository.findCustomers(pageable).map(UserMapper::toResponse);
	}

	public Page<UserResponse> searchCustomers(String keyword, Pageable pageable) {
		return userRepository.searchCustomers(keyword, pageable).map(UserMapper::toResponse);
	}

	public UserResponse get(UUID id) {
		return UserMapper.toResponse(findUser(id));
	}

	public UserResponse getCurrentProfile() {
		UUID userId = SecurityUtils.getCurrentUserId();
		if (userId == null) {
			throw new BusinessException("Not authenticated");
		}
		return get(userId);
	}

	private User findUser(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private Set<Role> resolveRoles(java.util.List<String> roleNames) {
		Set<Role> roles = new HashSet<>();
		for (String roleName : roleNames) {
			String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
			Role role = roleRepository.findByRoleName(normalized)
					.orElseThrow(() -> new ResourceNotFoundException("Role not found: " + normalized));
			roles.add(role);
		}
		return roles;
	}
}
