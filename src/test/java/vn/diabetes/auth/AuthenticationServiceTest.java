package vn.diabetes.auth;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import java.util.Optional;import models.User;import org.junit.jupiter.api.Test;
class AuthenticationServiceTest {
 @Test void authenticatesEncodedPassword(){UserRepository repo=mock(UserRepository.class);User user=new User();user.setUserId(1);user.setUsername("staff01");user.setPassword(Passwords.encode("Staff@123"));when(repo.findActiveByUsername("staff01")).thenReturn(Optional.of(user));AuthenticationService.LoginResult result=new AuthenticationService(repo).login("staff01","Staff@123");assertTrue(result.successful());assertEquals(user,result.user());}
 @Test void rejectsWrongPassword(){UserRepository repo=mock(UserRepository.class);when(repo.findActiveByUsername("missing")).thenReturn(Optional.empty());AuthenticationService.LoginResult result=new AuthenticationService(repo).login("missing","wrong");assertFalse(result.successful());assertNotNull(result.error());}
 @Test void acceptsPlainTextPassword(){UserRepository repo=mock(UserRepository.class);User user=new User();user.setPassword("simple123");when(repo.findActiveByUsername("staff01")).thenReturn(Optional.of(user));assertTrue(new AuthenticationService(repo).login("staff01","simple123").successful());}
}
