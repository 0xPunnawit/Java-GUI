package shop;

public class Main {

    public static void main(String[] args) {
        DatabaseConnection.getConnection();
        MainMenu mainMenu = new MainMenu();
        mainMenu.setVisible(true);
    }

}
