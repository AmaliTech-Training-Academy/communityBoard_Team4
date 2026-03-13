package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.UpdateProfileRequest;
import com.amalitech.communityboard.dto.UserResponse;
import com.amalitech.communityboard.exception.BadRequestException;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        user.setName(request.getName());

        boolean changingPassword =
                request.getCurrentPassword() != null && !request.getCurrentPassword().isBlank()
                && request.getNewPassword() != null && !request.getNewPassword().isBlank();

        if (changingPassword) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }
}
