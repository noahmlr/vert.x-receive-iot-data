package data;

public class AdminUser {
  private final String username;
  private final String password;

  private boolean authenticated;

  public AdminUser(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
