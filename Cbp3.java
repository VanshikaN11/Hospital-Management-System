import java.sql.*;
import java.util.Scanner;

public class Cbp3 {

    private static final String URL = "jdbc:mysql://localhost:3306/demo"; 
    private static final String USER = "root"; 
    private static final String PASSWORD = "dbmscbp24"; 

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    //  function to handle new or existing patients
    public static void handlePatient() {
        try (Scanner scanner = new Scanner(System.in);
             Connection conn = getConnection()) {

            System.out.print("Enter Patient ID: ");
            int patient_id = scanner.nextInt();
            scanner.nextLine(); 
            
            String checkQuery = "SELECT * FROM patients WHERE patient_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                pstmt.setInt(1, patient_id);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                   
                    System.out.println("Welcome back! Fetching your appointment details...");
                    viewOldPatientAppointments(patient_id, conn);
                } else {
                    // New patient
                    System.out.println("New Patient! Registering your details...");
                    registerAndBookAppointment(patient_id, conn, scanner);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Register patient
    private static void registerAndBookAppointment(int patient_id, Connection conn, Scanner scanner) throws SQLException {
        System.out.print("First Name: ");
        String firstName = scanner.nextLine();

        System.out.print("Last Name: ");
        String lastName = scanner.nextLine();

        System.out.print("Gender: ");
        String gender = scanner.nextLine();

        System.out.print("Phone Number: ");
        String phoneNumber = scanner.nextLine();

        System.out.print("Address: ");
        String address = scanner.nextLine();

        System.out.print("Bank account: ");
        String bankaccount = scanner.nextLine();

        String insertPatient = "INSERT INTO patients (patient_id, firstname, lastname, gender, phonenumber, patient_address,bankaccount) VALUES (?, ?, ?, ?, ?, ?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertPatient)) {
            pstmt.setInt(1, patient_id);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, gender);
            pstmt.setString(5, phoneNumber);
            pstmt.setString(6, address);
            pstmt.setString(7, bankaccount);
            pstmt.executeUpdate();
            System.out.println("Patient registered successfully!");
        }

        // Book appointment
        System.out.print("Enter the doctor specialization you want to meet: ");
        String disease = scanner.nextLine();

        int doctor_id = getDoctorByProfession(disease, conn);
        if (doctor_id == -1) {
            System.out.println("No doctor available for the specified disease.");
            return;
        }

        System.out.print("Enter Appointment ID: ");
        int appointment_id = scanner.nextInt();
        scanner.nextLine(); 

        System.out.print("Enter Appointment Date (YYYY-MM-DD HH:MM:SS): ");
        String appointmentDate = scanner.nextLine();

        String insertAppointment = "INSERT INTO appointments (appointment_id, patient_id, doctor_id, appointmentdate, statusofpatient) VALUES (?, ?, ?, ?, 'Scheduled')";
        try (PreparedStatement pstmt = conn.prepareStatement(insertAppointment)) {
            pstmt.setInt(1, appointment_id);
            pstmt.setInt(2, patient_id);
            pstmt.setInt(3, doctor_id);
            pstmt.setString(4, appointmentDate);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Appointment successfully booked!");
            } else {
                System.out.println("Failed to book the appointment.");
            }
        }

        // Add medical history
        System.out.println("medical history of patient:");
        System.out.print("Enter Record ID: ");
        int record_id = scanner.nextInt();
        scanner.nextLine(); 
        System.out.print("Enter Diagnosis: ");
        String diagnosis = scanner.nextLine();

        System.out.print("Enter Prescription: ");
        String prescription = scanner.nextLine();

        System.out.print("Enter Date of Visit (YYYY-MM-DD): ");
        String dateOfVisit = scanner.nextLine();

        String insertMedicalHistory = "INSERT INTO medicalhistory (record_id, patient_id, doctor_id, diagnosis, prescription, dateofvisit) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertMedicalHistory)) {
            pstmt.setInt(1, record_id);
            pstmt.setInt(2, patient_id);
            pstmt.setInt(3, doctor_id);
            pstmt.setString(4, diagnosis);
            pstmt.setString(5, prescription);
            pstmt.setString(6, dateOfVisit);
            pstmt.executeUpdate();
            System.out.println("Medical history added successfully!");
        }

        // Check and add insurance if eligible
        System.out.println("Checking insurance eligibility...");
        if (checkInsuranceEligibility(conn, patient_id)) {
            System.out.println("Insurance is applicable for the patient.");
            addInsuranceDetails(conn, scanner, patient_id);
        } else {
            System.out.println("Insurance is not applicable for the patient.");
        }
    }

    // Retrieve doctor by profession
    private static int getDoctorByProfession(String disease, Connection conn) throws SQLException {
        String query = "SELECT doctor_id FROM doctor WHERE speciality = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, disease);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("doctor_id");
            }
        }
        return -1; 
    }

    // View appointments for an existing patient
    private static void viewOldPatientAppointments(int patient_id, Connection conn) throws SQLException {
        String query = """
                SELECT a.appointment_id, d.firstname AS doctor_name, a.appointmentdate, a.statusofpatient
                FROM appointments a
                JOIN doctor d ON a.doctor_id = d.doctor_id
                WHERE a.patient_id = ?;
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patient_id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("No appointments found for the given Patient ID.");
                    return;
                }

                System.out.printf("%-15s %-20s %-15s %-15s%n", "Appointment ID", "Doctor Name", "Date", "Status");
                System.out.println("----------------------------------------------");

                while (rs.next()) {
                    System.out.printf("%-15d %-20s %-15s %-15s%n",
                            rs.getInt("appointment_id"),
                            rs.getString("doctor_name"),
                            rs.getDate("appointmentdate"),
                            rs.getString("statusofpatient"));
                }
            }
        }
    }

    // Check insurance eligibility based on medical history
    private static boolean checkInsuranceEligibility(Connection conn, int patient_id) throws SQLException {
        String query = """
                SELECT m.diagnosis 
                FROM medicalhistory m
                WHERE m.patient_id = ?
                ORDER BY m.dateofvisit DESC
                LIMIT 1;
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patient_id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String diagnosis = rs.getString("diagnosis");
                String[] eligibleDiseases = {"hypertension", "hand fracture", "heart attack"};
                for (String disease : eligibleDiseases) {
                    if (diagnosis.equalsIgnoreCase(disease)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Add insurance details
    private static void addInsuranceDetails(Connection conn, Scanner scanner, int patient_id) throws SQLException {
        System.out.print("Enter Insurance Provider: ");
        String insuranceProvider = scanner.nextLine();

        System.out.print("Enter Policy Number: ");
        String policyNumber = scanner.nextLine();

        System.out.print("Enter Policyholder Name: ");
        String policyholderName = scanner.nextLine();

        System.out.print("Enter Expiry Date (YYYY-MM-DD): ");
        String expiryDate = scanner.nextLine();

        String insertInsurance = "INSERT INTO insurance (patient_id, insuranceprovider, policynumber, policyholdername, expirydate) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertInsurance)) {
            pstmt.setInt(1, patient_id);
            pstmt.setString(2, insuranceProvider);
            pstmt.setString(3, policyNumber);
            pstmt.setString(4, policyholderName);
            pstmt.setString(5, expiryDate);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Insurance details added successfully!");
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- Hospital Management System ---");
            System.out.println("1. Handle Patient (New or Existing)");
            System.out.println("2. Check Insurance Eligibility");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1 -> handlePatient();
                case 2 -> {
                    System.out.print("Enter Patient ID: ");
                    int patient_id = scanner.nextInt();
                    try (Connection conn = getConnection()) {
                        if (checkInsuranceEligibility(conn, patient_id)) {
                            System.out.println("Patient is eligible for insurance.");
                        } else {
                            System.out.println("Patient is NOT eligible for insurance.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case 3 -> {
                    exit = true;
                    System.out.println("Thank you visiting Goodbye!");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }

        
    }
}
