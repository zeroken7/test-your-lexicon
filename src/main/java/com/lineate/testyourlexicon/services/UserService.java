package com.lineate.testyourlexicon.services;

import com.lineate.testyourlexicon.dto.UserDto;
import com.lineate.testyourlexicon.dto.UserRegistrationDto;
import com.lineate.testyourlexicon.entities.GameConfiguration;
import com.lineate.testyourlexicon.entities.User;
import com.lineate.testyourlexicon.repositories.GameConfigurationRepository;
import com.lineate.testyourlexicon.repositories.UserRepository;
import com.lineate.testyourlexicon.util.GameUtil;
import com.lineate.testyourlexicon.util.Hash;
import com.lineate.testyourlexicon.util.UserMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

  private final GameConfigurationRepository gameConfigurationRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  private void logUserRegistration(UserRegistrationDto userRegistrationDto) {
    log.info("Registered user: {first name: {}, last name: {}, email: {}}",
        userRegistrationDto.getFirstName().toLowerCase(),
        userRegistrationDto.getLastName().toLowerCase(),
        userRegistrationDto.getEmail());
  }

  public UserDto createUser(UserRegistrationDto userRegistrationDto) {
    if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmationPassword())) {
      throw new IllegalArgumentException("Passwords do not match");
    }

    User registeredUser = userMapper.userRegistrationDtoToUser(userRegistrationDto);

    try {
      userRepository.save(registeredUser);
    } catch (DataAccessException ex) {
      throw new IllegalArgumentException("Email is already in use!");
    }

    GameConfiguration defaultGameConfiguration = GameUtil.defaultGameConfiguration();
    defaultGameConfiguration.setUserHash(Hash.hashToLong(registeredUser.getId()));
    gameConfigurationRepository.save(defaultGameConfiguration);

    logUserRegistration(userRegistrationDto);

    return UserMapper.userToUserDto(registeredUser);
  }

  public List<UserDto> getAll() {
    return userRepository.findAll().stream()
      .map(UserMapper::userToUserDto)
      .collect(Collectors.toList());
  }

  public User findUserByEmail(String email) {
    return userRepository.findUserByEmail(email).get();
  }
}
