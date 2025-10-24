package demo.user;

import com.example.demo.user.AuthController;
import com.example.demo.user.AuthService;
import com.example.demo.user.User;
import com.example.demo.user.UserRegistrationRequest;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void whenRegisterUser_withValidRequest_thenReturnsCreated() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Password123");
        User registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setUsername("test@example.com");
        registeredUser.setRole(User.Role.USER);
        registeredUser.setTenantId(1L);

        when(authService.register(any(UserRegistrationRequest.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.tenantId").value(1L))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void whenRegisterUser_withExistingUsername_thenReturnsConflict() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("exists@example.com", "Password123");

        doThrow(new UserAlreadyExistsException("Username already exists."))
                .when(authService).register(any(UserRegistrationRequest.class));

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // 409 conflict
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    @Test
    void whenRegisterUser_withInvalidEmail_thenReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("bad_email", "Password123");

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
