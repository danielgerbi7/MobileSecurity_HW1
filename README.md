# 📱 Mobile Security – Android App

This Android application was created as part of a **mobile security HW1**.  
The goal is to simulate a secure entry system based on **5 unique device/environmental conditions**.


## ✅ App Flow

- The main screen displays 5 toggle switches (representing the required conditions).
- The **"Continue"** button is enabled only when **all conditions are satisfied**.
- When the user taps "Continue", the app navigates to a **success screen** displaying a congratulatory message.



## 🔐 Access Conditions

| # | Condition              | Description                                                                 |
|---|------------------------|-----------------------------------------------------------------------------|
| 1 | Wi-Fi Nearby           | Device must detect a Wi-Fi network with the name `Stern5g`.                |
| 2 | Battery Level          | Battery must be at least **50%**.                                          |
| 3 | Bluetooth ON           | Bluetooth must be turned **on**.                                           |
| 4 | Noisy Environment      | The surrounding environment must be **loud**, detected via the microphone. |
| 5 | Ringer Mode Normal     | Device must **not** be in silent or vibrate mode.                          |

## ▶️ How to Run

1. Clone the repository
2. Open in Android Studio
3. Run on a real Android device (microphone, Wi-Fi, Bluetooth required)

## 📱 Screenshots

**1. No Conditions Met**

| <img src="Images/BeforeConditions.jpg" alt=" Before conditions - Light Mode" width="300"/> |
|--------------------------------------------------------------------------------------------|

**2. All Conditions Met – Ready to Continue**

| <img src="Images/AfterConditions.jpg" alt=" After conditions - Light Mode" width="300"/> |
|------------------------------------------------------------------------------------------|

**3. Success Screen**

| <img src="Images/SuccessScreen.jpg" alt=" Success Screen - Light Mode" width="300"/> |
|--------------------------------------------------------------------------------------|

## Author

[Daniel Gerbi](https://github.com/danielgerbi7)