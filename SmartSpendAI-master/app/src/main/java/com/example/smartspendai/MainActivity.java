package com.example.smartspendai;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartspendai.data.model.BudgetCategory;
import com.example.smartspendai.data.model.BudgetSummary;
import com.example.smartspendai.data.model.Expense;
import com.example.smartspendai.data.model.AiAdviceRecord;
import com.example.smartspendai.data.remote.AiAdviceService;
import com.example.smartspendai.data.model.User;
import com.example.smartspendai.data.repository.AiAdviceRepository;
import com.example.smartspendai.data.repository.BudgetRepository;
import com.example.smartspendai.data.repository.ExpenseRepository;
import com.example.smartspendai.data.repository.UserRepository;
import com.example.smartspendai.ui.SpendingPieChartView;
import com.example.smartspendai.util.CurrencyFormatter;
import com.example.smartspendai.util.ReceiptTextParser;
import com.example.smartspendai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private UserRepository userRepository;
    private BudgetRepository budgetRepository;
    private ExpenseRepository expenseRepository;
    private AiAdviceRepository aiAdviceRepository;
    private AiAdviceService aiAdviceService;
    private SessionManager sessionManager;
    private int currentMonth;
    private int currentYear;
    private int historyMonth;
    private int historyYear;
    private Long historyDateStartMillis;
    private Long historyDateEndMillis;
    private ActivityResultLauncher<Void> takeReceiptPhotoLauncher;
    private ActivityResultLauncher<String> pickReceiptFromGalleryLauncher;
    private Spinner addExpenseCategorySpinner;
    private TextInputEditText expenseTitleEditText;
    private TextInputEditText expenseAmountEditText;
    private TextInputEditText expenseNoteEditText;
    private TextView receiptScanStatusText;
    private TextView receiptPreviewTitleText;
    private TextView receiptPreviewAmountText;
    private TextView receiptPreviewMetaText;
    private ProgressBar receiptScanProgressBar;
    private ImageView receiptPreviewImage;
    private View receiptPreviewCard;
    private List<BudgetCategory> addExpenseCategories = new ArrayList<>();
    private final List<AiChatMessage> aiChatMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        takeReceiptPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                this::handleReceiptCameraResult
        );
        pickReceiptFromGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleReceiptGalleryResult
        );
        userRepository = new UserRepository(this);
        budgetRepository = new BudgetRepository(this);
        expenseRepository = new ExpenseRepository(this);
        aiAdviceRepository = new AiAdviceRepository(this);
        aiAdviceService = new AiAdviceService();
        sessionManager = new SessionManager(this);
        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentYear = calendar.get(Calendar.YEAR);

        if (sessionManager.getCurrentUser() == null) {
            showLoginScreen();
        } else {
            showHomeScreen();
        }
    }

    private void showLoginScreen() {
        setContentView(R.layout.activity_main);
        applyWindowInsets(findViewById(R.id.main));

        TextInputEditText emailEditText = findViewById(R.id.loginEmailEditText);
        TextInputEditText passwordEditText = findViewById(R.id.loginPasswordEditText);
        MaterialButton loginButton = findViewById(R.id.loginButton);
        MaterialButton openRegisterButton = findViewById(R.id.openRegisterButton);

        loginButton.setOnClickListener(v -> {
            String email = readText(emailEditText);
            String password = readText(passwordEditText);

            if (!isValidEmail(email)) {
                showMessage("Enter a valid email address.");
                return;
            }

            if (password.isEmpty()) {
                showMessage("Enter your password.");
                return;
            }

            User user = userRepository.login(email, password);
            if (user == null) {
                showMessage("Invalid email or password.");
                return;
            }

            sessionManager.saveUser(user);
            showHomeScreen();
        });

        openRegisterButton.setOnClickListener(v -> showRegisterScreen());
    }

    private void showRegisterScreen() {
        setContentView(R.layout.activity_register);
        applyWindowInsets(findViewById(R.id.registerRoot));

        TextInputEditText nameEditText = findViewById(R.id.registerNameEditText);
        TextInputEditText emailEditText = findViewById(R.id.registerEmailEditText);
        TextInputEditText passwordEditText = findViewById(R.id.registerPasswordEditText);
        TextInputEditText confirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText);
        MaterialButton registerButton = findViewById(R.id.registerButton);
        MaterialButton openLoginButton = findViewById(R.id.openLoginButton);

        registerButton.setOnClickListener(v -> {
            String name = readText(nameEditText);
            String email = readText(emailEditText);
            String password = readText(passwordEditText);
            String confirmPassword = readText(confirmPasswordEditText);

            if (name.length() < 2) {
                showMessage("Enter your full name.");
                return;
            }

            if (!isValidEmail(email)) {
                showMessage("Enter a valid email address.");
                return;
            }

            if (password.length() < 6) {
                showMessage("Password must be at least 6 characters.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showMessage("Passwords do not match.");
                return;
            }

            UserRepository.RegisterResult result = userRepository.register(name, email, password);
            if (!result.isSuccess()) {
                showMessage(result.getErrorMessage());
                return;
            }

            sessionManager.saveUser(result.getUser());
            showHomeScreen();
        });

        openLoginButton.setOnClickListener(v -> showLoginScreen());
    }

    private void showHomeScreen() {
        setContentView(R.layout.activity_home);
        applyWindowInsets(findViewById(R.id.homeRoot));
        setupBottomNavigation(R.id.nav_home);

        User user = sessionManager.getCurrentUser();
        TextView greetingText = findViewById(R.id.homeGreetingText);
        TextView nameText = findViewById(R.id.homeNameText);
        TextView initialText = findViewById(R.id.homeInitialText);
        TextView budgetMonthText = findViewById(R.id.budgetMonthText);
        TextView budgetStatusText = findViewById(R.id.budgetStatusText);
        TextView incomeSummaryText = findViewById(R.id.incomeSummaryText);
        TextView allocatedSummaryText = findViewById(R.id.allocatedSummaryText);
        TextView spentSummaryText = findViewById(R.id.spentSummaryText);
        TextView unallocatedSummaryText = findViewById(R.id.unallocatedSummaryText);
        TextView healthScoreText = findViewById(R.id.healthScoreText);
        TextView overspendingAlertText = findViewById(R.id.overspendingAlertText);
        View budgetSummaryLayout = findViewById(R.id.budgetSummaryLayout);
        MaterialButton setupBudgetButton = findViewById(R.id.setupBudgetButton);
        MaterialButton logoutButton = findViewById(R.id.logoutButton);

        if (user != null) {
            greetingText.setText(R.string.home_title);
            nameText.setText(user.getName());
            initialText.setText(getInitial(user.getName()));
            budgetMonthText.setText(getCurrentMonthLabel());

            BudgetSummary budgetSummary = budgetRepository.getMonthlyBudget(
                    user.getId(),
                    currentMonth,
                    currentYear
            );

            if (budgetSummary == null) {
                budgetStatusText.setText(R.string.no_budget_yet);
                unallocatedSummaryText.setText(CurrencyFormatter.format(0));
                budgetSummaryLayout.setVisibility(View.GONE);
                setupBudgetButton.setText(R.string.set_up_budget);
            } else {
                double remainingBalance = budgetSummary.getIncome() - budgetSummary.getTotalSpent();
                budgetStatusText.setText("Remaining balance for this month");
                unallocatedSummaryText.setText(CurrencyFormatter.format(remainingBalance));
                budgetSummaryLayout.setVisibility(View.VISIBLE);
                incomeSummaryText.setText("Income\n" + CurrencyFormatter.format(budgetSummary.getIncome()));
                allocatedSummaryText.setText("Allocated "
                        + CurrencyFormatter.format(budgetSummary.getTotalAllocated())
                        + " | Unallocated "
                        + CurrencyFormatter.format(budgetSummary.getUnallocatedAmount()));
                spentSummaryText.setText("Spent\n"
                        + CurrencyFormatter.format(budgetSummary.getTotalSpent()));
                healthScoreText.setText(buildStyledBudgetHealthScore(budgetSummary));
                CharSequence alertText = buildStyledOverspendingAlerts(budgetSummary);
                if (alertText.length() == 0) {
                    overspendingAlertText.setVisibility(View.GONE);
                } else {
                    overspendingAlertText.setVisibility(View.VISIBLE);
                    overspendingAlertText.setText(alertText);
                }
                setupBudgetButton.setText(R.string.edit_budget);
            }
        }

        setupBudgetButton.setOnClickListener(v -> showBudgetSetupScreen());

        logoutButton.setOnClickListener(v -> {
            sessionManager.clear();
            showLoginScreen();
        });
    }

    private void showBudgetSetupScreen() {
        setContentView(R.layout.activity_budget_setup);
        applyWindowInsets(findViewById(R.id.budgetSetupRoot));

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        TextView monthText = findViewById(R.id.budgetSetupMonthText);
        TextView helperText = findViewById(R.id.allocationHelperText);
        TextInputEditText incomeEditText = findViewById(R.id.monthlyIncomeEditText);
        Map<String, TextInputEditText> categoryInputs = getCategoryInputs();
        MaterialButton backButton = findViewById(R.id.budgetSetupBackButton);
        MaterialButton saveBudgetButton = findViewById(R.id.saveBudgetButton);
        MaterialButton cancelBudgetButton = findViewById(R.id.cancelBudgetButton);

        monthText.setText(getCurrentMonthLabel());

        BudgetSummary existingBudget = budgetRepository.getMonthlyBudget(
                user.getId(),
                currentMonth,
                currentYear
        );
        if (existingBudget != null) {
            incomeEditText.setText(formatInputAmount(existingBudget.getIncome()));
            for (BudgetCategory category : existingBudget.getCategories()) {
                TextInputEditText input = categoryInputs.get(category.getName());
                if (input != null) {
                    input.setText(formatInputAmount(category.getAllocatedAmount()));
                }
            }
        }

        helperText.setText("Total allocation must be less than or equal to your income.");

        backButton.setOnClickListener(v -> showHomeScreen());
        saveBudgetButton.setOnClickListener(v -> {
            double income = parseAmount(readText(incomeEditText));
            if (income <= 0) {
                showMessage("Enter a monthly income greater than 0.");
                return;
            }

            Map<String, Double> allocations = new LinkedHashMap<>();
            double totalAllocated = 0;
            for (Map.Entry<String, TextInputEditText> entry : categoryInputs.entrySet()) {
                double amount = parseAmount(readText(entry.getValue()));
                if (amount < 0) {
                    showMessage("Budget amounts cannot be negative.");
                    return;
                }
                allocations.put(entry.getKey(), amount);
                totalAllocated += amount;
            }

            if (totalAllocated <= 0) {
                showMessage("Allocate at least one category.");
                return;
            }

            if (totalAllocated > income) {
                showMessage("Allocated amount cannot exceed monthly income.");
                return;
            }

            budgetRepository.saveMonthlyBudget(user.getId(), currentMonth, currentYear, income, allocations);
            showMessage("Budget saved.");
            showHomeScreen();
        });

        cancelBudgetButton.setOnClickListener(v -> showHomeScreen());
    }

    private void showAddExpenseScreen() {
        setContentView(R.layout.activity_add_expense);
        applyWindowInsets(findViewById(R.id.addExpenseRoot));
        setupBottomNavigation(R.id.nav_add);

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        BudgetSummary budgetSummary = budgetRepository.getMonthlyBudget(
                user.getId(),
                currentMonth,
                currentYear
        );
        if (budgetSummary == null) {
            showMessage("Set up this month's budget first.");
            showHomeScreen();
            return;
        }

        TextView monthText = findViewById(R.id.addExpenseMonthText);
        TextView helperText = findViewById(R.id.expenseHelperText);
        Spinner categorySpinner = findViewById(R.id.categorySpinner);
        TextInputEditText titleEditText = findViewById(R.id.expenseTitleEditText);
        TextInputEditText amountEditText = findViewById(R.id.expenseAmountEditText);
        TextInputEditText noteEditText = findViewById(R.id.expenseNoteEditText);
        MaterialButton scanReceiptCameraButton = findViewById(R.id.scanReceiptCameraButton);
        MaterialButton scanReceiptGalleryButton = findViewById(R.id.scanReceiptGalleryButton);
        MaterialButton backButton = findViewById(R.id.addExpenseBackButton);
        MaterialButton saveExpenseButton = findViewById(R.id.saveExpenseButton);
        MaterialButton cancelExpenseButton = findViewById(R.id.cancelExpenseButton);
        addExpenseCategorySpinner = categorySpinner;
        expenseTitleEditText = titleEditText;
        expenseAmountEditText = amountEditText;
        expenseNoteEditText = noteEditText;
        receiptScanProgressBar = findViewById(R.id.receiptScanProgressBar);
        receiptScanStatusText = findViewById(R.id.receiptScanStatusText);
        receiptPreviewImage = findViewById(R.id.receiptPreviewImage);
        receiptPreviewCard = findViewById(R.id.receiptPreviewCard);
        receiptPreviewTitleText = findViewById(R.id.receiptPreviewTitleText);
        receiptPreviewAmountText = findViewById(R.id.receiptPreviewAmountText);
        receiptPreviewMetaText = findViewById(R.id.receiptPreviewMetaText);
        monthText.setText(getCurrentMonthLabel());
        List<BudgetCategory> categories = budgetSummary.getCategories();
        addExpenseCategories = categories;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                buildCategorySpinnerLabels(categories)
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        categorySpinner.setAdapter(adapter);
        helperText.setText("Scan a receipt for a quick amount fill, then review the category, title and note before saving.");
        resetReceiptPreview();

        backButton.setOnClickListener(v -> showHomeScreen());
        scanReceiptCameraButton.setOnClickListener(v -> {
            showReceiptStatus("Opening camera. Hold the receipt steady and keep the total visible.", false);
            takeReceiptPhotoLauncher.launch(null);
        });
        scanReceiptGalleryButton.setOnClickListener(v -> {
            showReceiptStatus("Choose a receipt image from your album.", false);
            pickReceiptFromGalleryLauncher.launch("image/*");
        });
        saveExpenseButton.setOnClickListener(v -> {
            int selectedPosition = categorySpinner.getSelectedItemPosition();
            if (selectedPosition < 0 || selectedPosition >= categories.size()) {
                showMessage("Choose a category.");
                return;
            }

            BudgetCategory selectedCategory = categories.get(selectedPosition);
            String title = readText(titleEditText);
            double amount = parseAmount(readText(amountEditText));
            String note = readText(noteEditText);

            if (title.length() < 2) {
                showMessage("Enter an expense title.");
                return;
            }

            if (amount <= 0) {
                showMessage("Enter an amount greater than 0.");
                return;
            }

            expenseRepository.addExpense(
                    user.getId(),
                    budgetSummary.getId(),
                    selectedCategory.getId(),
                    title,
                    amount,
                    note,
                    System.currentTimeMillis()
            );

            if (amount > selectedCategory.getRemainingAmount()) {
                showMessage("Expense saved. Warning: this category is over budget.");
            } else {
                showMessage("Expense saved.");
            }
            showHomeScreen();
        });

        cancelExpenseButton.setOnClickListener(v -> showHomeScreen());
    }

    private void showReportScreen() {
        setContentView(R.layout.activity_report);
        applyWindowInsets(findViewById(R.id.reportRoot));
        setupBottomNavigation(R.id.nav_report);

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        BudgetSummary budgetSummary = budgetRepository.getMonthlyBudget(
                user.getId(),
                currentMonth,
                currentYear
        );
        if (budgetSummary == null) {
            showMessage("Set up this month's budget first.");
            showHomeScreen();
            return;
        }

        TextView monthText = findViewById(R.id.reportMonthText);
        TextView overviewText = findViewById(R.id.reportOverviewText);
        SpendingPieChartView spendingPieChartView = findViewById(R.id.spendingPieChartView);
        LinearLayout spendingLegendContainer = findViewById(R.id.spendingChartLegendContainer);
        LinearLayout categoryContainer = findViewById(R.id.categoryReportContainer);
        MaterialButton backButton = findViewById(R.id.reportBackButton);

        monthText.setText(getCurrentMonthLabel());
        overviewText.setText(buildStyledReportOverview(budgetSummary));
        populateSpendingChart(spendingPieChartView, spendingLegendContainer, budgetSummary.getCategories());
        populateCategoryReport(categoryContainer, budgetSummary.getCategories());
        backButton.setOnClickListener(v -> showHomeScreen());
    }

    private void showAiAdviceScreen() {
        setContentView(R.layout.activity_ai_advice);
        applyWindowInsets(findViewById(R.id.aiAdviceRoot));
        setupBottomNavigation(R.id.nav_ai);

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        BudgetSummary budgetSummary = budgetRepository.getMonthlyBudget(
                user.getId(),
                currentMonth,
                currentYear
        );
        if (budgetSummary == null) {
            showMessage("Set up this month's budget first.");
            showHomeScreen();
            return;
        }

        TextView monthText = findViewById(R.id.aiAdviceMonthText);
        TextView statusText = findViewById(R.id.adviceStatusText);
        TextInputEditText questionInput = findViewById(R.id.adviceQuestionInput);
        ScrollView conversationScrollView = findViewById(R.id.conversationScrollView);
        LinearLayout conversationContainer = findViewById(R.id.conversationContainer);
        MaterialButton headerBackButton = findViewById(R.id.aiAdviceBackButton);
        MaterialButton generateAdviceButton = findViewById(R.id.generateAdviceButton);

        monthText.setText(getCurrentMonthLabel());
        statusText.setText(aiChatMessages.isEmpty()
                ? "SmartSpend Assistant is online."
                : "Session chat stays here until you close the app.");
        renderAiChatConversation(conversationContainer, conversationScrollView);

        generateAdviceButton.setOnClickListener(v -> {
            String question = questionInput.getText() == null
                    ? ""
                    : questionInput.getText().toString().trim();
            if (TextUtils.isEmpty(question)) {
                questionInput.setError("Ask a budgeting question first.");
                questionInput.requestFocus();
                return;
            }

            questionInput.setError(null);
            generateAdviceButton.setEnabled(false);
            questionInput.setText("");
            statusText.setText("Thinking...");

            aiChatMessages.add(new AiChatMessage(true, question));
            aiChatMessages.add(new AiChatMessage(false, "Let me look at your budget and think through that."));
            renderAiChatConversation(conversationContainer, conversationScrollView);

            aiAdviceService.requestBudgetAdvice(budgetSummary, question, new AiAdviceService.AdviceCallback() {
                @Override
                public void onSuccess(int statusCode, String advice) {
                    generateAdviceButton.setEnabled(true);
                    statusText.setText("Session chat stays here until you close the app.");
                    replaceLastAssistantMessage(advice);
                    renderAiChatConversation(conversationContainer, conversationScrollView);
                }

                @Override
                public void onError(String errorMessage) {
                    generateAdviceButton.setEnabled(true);
                    statusText.setText("I could not answer just now.");
                    replaceLastAssistantMessage("Please check your Firebase AI setup, project API access, and internet connection.\n\nDetails: " + errorMessage);
                    renderAiChatConversation(conversationContainer, conversationScrollView);
                }
            });
        });

        headerBackButton.setOnClickListener(v -> showHomeScreen());
    }

    private void showAdviceHistoryScreen() {
        setContentView(R.layout.activity_advice_history);
        applyWindowInsets(findViewById(R.id.adviceHistoryRoot));

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        BudgetSummary budgetSummary = budgetRepository.getMonthlyBudget(
                user.getId(),
                currentMonth,
                currentYear
        );
        if (budgetSummary == null) {
            showMessage("Set up this month's budget first.");
            showHomeScreen();
            return;
        }

        TextView monthText = findViewById(R.id.adviceHistoryMonthText);
        LinearLayout historyContainer = findViewById(R.id.adviceHistoryContainer);
        MaterialButton backButton = findViewById(R.id.adviceHistoryBackButton);

        monthText.setText(getCurrentMonthLabel());
        populateAdviceHistory(historyContainer, budgetSummary.getId());
        backButton.setOnClickListener(v -> showHomeScreen());
    }

    private void showExpenseHistoryScreen() {
        historyMonth = currentMonth;
        historyYear = currentYear;
        historyDateStartMillis = null;
        historyDateEndMillis = null;
        showExpenseHistoryScreenWithFilter();
    }

    private void showExpenseHistoryScreenWithFilter() {
        setContentView(R.layout.activity_expense_history);
        applyWindowInsets(findViewById(R.id.expenseHistoryRoot));
        setupBottomNavigation(R.id.nav_history);

        User user = sessionManager.getCurrentUser();
        if (user == null) {
            showLoginScreen();
            return;
        }

        TextView monthText = findViewById(R.id.expenseHistoryMonthText);
        TextView filterStatusText = findViewById(R.id.expenseFilterStatusText);
        LinearLayout historyContainer = findViewById(R.id.expenseHistoryContainer);
        MaterialButton backButton = findViewById(R.id.expenseHistoryBackButton);
        MaterialButton previousMonthButton = findViewById(R.id.previousMonthButton);
        MaterialButton nextMonthButton = findViewById(R.id.nextMonthButton);
        MaterialButton pickDateButton = findViewById(R.id.pickDateButton);
        MaterialButton clearDateFilterButton = findViewById(R.id.clearDateFilterButton);

        monthText.setText(getMonthLabel(historyMonth, historyYear));
        filterStatusText.setText(buildExpenseFilterStatus());
        populateExpenseHistory(historyContainer, getFilteredExpenses(user.getId()));

        previousMonthButton.setOnClickListener(v -> {
            shiftHistoryMonth(-1);
            historyDateStartMillis = null;
            historyDateEndMillis = null;
            showExpenseHistoryScreenWithFilter();
        });

        nextMonthButton.setOnClickListener(v -> {
            shiftHistoryMonth(1);
            historyDateStartMillis = null;
            historyDateEndMillis = null;
            showExpenseHistoryScreenWithFilter();
        });

        pickDateButton.setOnClickListener(v -> showExpenseDatePicker());

        clearDateFilterButton.setOnClickListener(v -> {
            historyDateStartMillis = null;
            historyDateEndMillis = null;
            showExpenseHistoryScreenWithFilter();
        });

        backButton.setOnClickListener(v -> showHomeScreen());
    }

    private void applyWindowInsets(View rootView) {
        final int initialLeftPadding = rootView.getPaddingLeft();
        final int initialTopPadding = rootView.getPaddingTop();
        final int initialRightPadding = rootView.getPaddingRight();
        final int initialBottomPadding = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeftPadding,
                    initialTopPadding + systemBars.top,
                    initialRightPadding,
                    initialBottomPadding + systemBars.bottom
            );
            return insets;
        });
    }

    private void setupBottomNavigation(int selectedItemId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setSelectedItemId(selectedItemId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) {
                return true;
            }

            if (itemId == R.id.nav_home) {
                showHomeScreen();
                return true;
            }

            if (itemId == R.id.nav_add) {
                showAddExpenseScreen();
                return true;
            }

            if (itemId == R.id.nav_report) {
                showReportScreen();
                return true;
            }

            if (itemId == R.id.nav_history) {
                showExpenseHistoryScreen();
                return true;
            }

            if (itemId == R.id.nav_ai) {
                showAiAdviceScreen();
                return true;
            }

            return false;
        });
    }

    private void handleReceiptCameraResult(Bitmap bitmap) {
        if (bitmap == null) {
            showReceiptStatus("No photo was captured. You can try again or choose one from your album.", false);
            return;
        }

        if (!isAddExpenseScreenActive()) {
            return;
        }

        receiptPreviewImage.setImageBitmap(bitmap);
        receiptPreviewImage.setVisibility(View.VISIBLE);
        scanReceiptImage(InputImage.fromBitmap(bitmap, 0));
    }

    private void handleReceiptGalleryResult(Uri uri) {
        if (uri == null) {
            showReceiptStatus("No image was selected. You can take a photo or choose another receipt.", false);
            return;
        }

        if (!isAddExpenseScreenActive()) {
            return;
        }

        receiptPreviewImage.setImageURI(uri);
        receiptPreviewImage.setVisibility(View.VISIBLE);
        try {
            scanReceiptImage(InputImage.fromFilePath(this, uri));
        } catch (IOException exception) {
            showReceiptStatus("We couldn't open that receipt image. Please try another photo.", false);
        }
    }

    private void scanReceiptImage(InputImage inputImage) {
        if (!isAddExpenseScreenActive()) {
            return;
        }

        showReceiptStatus("Scanning receipt and looking for the total amount...", true);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(inputImage)
                .addOnSuccessListener(visionText -> applyReceiptScanResult(ReceiptTextParser.parse(visionText.getText())))
                .addOnFailureListener(exception -> {
                    showReceiptStatus("The receipt could not be read clearly. Try a brighter image or type it in manually.", false);
                    if (receiptPreviewCard != null) {
                        receiptPreviewCard.setVisibility(View.GONE);
                    }
                })
                .addOnCompleteListener(task -> recognizer.close());
    }

    private void applyReceiptScanResult(ReceiptTextParser.ReceiptScanResult result) {
        if (!isAddExpenseScreenActive()) {
            return;
        }

        if (!result.hasAnySignal()) {
            showReceiptStatus("We couldn't confidently detect receipt details. You can still fill the form manually.", false);
            receiptPreviewCard.setVisibility(View.GONE);
            return;
        }

        receiptPreviewCard.setVisibility(View.VISIBLE);
        receiptPreviewTitleText.setText(
                TextUtils.isEmpty(result.getMerchantName()) ? "Receipt ready to review" : result.getMerchantName()
        );
        receiptPreviewAmountText.setText(result.hasDetectedAmount()
                ? CurrencyFormatter.format(result.getDetectedAmount())
                : "Amount not found");
        receiptPreviewMetaText.setText(buildReceiptPreviewMeta(result));
        showReceiptStatus(
                result.hasDetectedAmount()
                        ? "Receipt scanned. Please review the amount, category and notes before saving."
                        : "We found part of the receipt, but not a confident total yet. Please review and edit manually.",
                false
        );
        autofillExpenseForm(result);
    }

    private String buildReceiptPreviewMeta(ReceiptTextParser.ReceiptScanResult result) {
        List<String> details = new ArrayList<>();
        if (!TextUtils.isEmpty(result.getDetectedDate())) {
            details.add("Receipt date: " + result.getDetectedDate());
        }
        if (!TextUtils.isEmpty(result.getSuggestedCategory())) {
            details.add("Suggested category: " + getCategoryLabel(result.getSuggestedCategory()));
        }
        if (!TextUtils.isEmpty(result.getMatchedLine())) {
            details.add("Matched line: " + result.getMatchedLine());
        } else if (!result.getCandidateAmounts().isEmpty()) {
            details.add("Other amounts seen: " + TextUtils.join(", ", result.getCandidateAmounts()));
        }
        return TextUtils.join("\n", details);
    }

    private void autofillExpenseForm(ReceiptTextParser.ReceiptScanResult result) {
        if (!isAddExpenseScreenActive()) {
            return;
        }

        if (result.hasDetectedAmount() && expenseAmountEditText != null) {
            expenseAmountEditText.setText(formatInputAmount(result.getDetectedAmount()));
        }

        if (expenseTitleEditText != null) {
            String currentTitle = readText(expenseTitleEditText);
            if (currentTitle.isEmpty() || "Receipt purchase".equalsIgnoreCase(currentTitle)) {
                String suggestedTitle = TextUtils.isEmpty(result.getMerchantName())
                        ? "Receipt purchase"
                        : result.getMerchantName();
                expenseTitleEditText.setText(suggestedTitle);
            }
        }

        if (expenseNoteEditText != null) {
            String note = buildReceiptNote(result);
            if (!TextUtils.isEmpty(note) && readText(expenseNoteEditText).isEmpty()) {
                expenseNoteEditText.setText(note);
            }
        }

        if (!TextUtils.isEmpty(result.getSuggestedCategory())) {
            selectCategory(result.getSuggestedCategory());
        }
    }

    private String buildReceiptNote(ReceiptTextParser.ReceiptScanResult result) {
        List<String> noteParts = new ArrayList<>();
        noteParts.add("Scanned from receipt");
        if (!TextUtils.isEmpty(result.getDetectedDate())) {
            noteParts.add("Date: " + result.getDetectedDate());
        }
        return TextUtils.join(" | ", noteParts);
    }

    private void selectCategory(String categoryName) {
        if (addExpenseCategorySpinner == null) {
            return;
        }

        for (int i = 0; i < addExpenseCategories.size(); i++) {
            if (categoryName.equalsIgnoreCase(addExpenseCategories.get(i).getName())) {
                addExpenseCategorySpinner.setSelection(i);
                return;
            }
        }
    }

    private void resetReceiptPreview() {
        if (receiptPreviewCard != null) {
            receiptPreviewCard.setVisibility(View.GONE);
        }
        if (receiptPreviewImage != null) {
            receiptPreviewImage.setImageDrawable(null);
            receiptPreviewImage.setVisibility(View.GONE);
        }
        showReceiptStatus("Tip: keep the full receipt in frame and avoid shadows for a more accurate scan.", false);
    }

    private void showReceiptStatus(String message, boolean loading) {
        if (receiptScanStatusText != null) {
            receiptScanStatusText.setText(message);
        }
        if (receiptScanProgressBar != null) {
            receiptScanProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isAddExpenseScreenActive() {
        return findViewById(R.id.addExpenseRoot) != null
                && expenseAmountEditText != null
                && addExpenseCategorySpinner != null;
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Map<String, TextInputEditText> getCategoryInputs() {
        Map<String, TextInputEditText> inputs = new LinkedHashMap<>();
        inputs.put("Food", findViewById(R.id.foodBudgetEditText));
        inputs.put("Transport", findViewById(R.id.transportBudgetEditText));
        inputs.put("Bills", findViewById(R.id.billsBudgetEditText));
        inputs.put("Entertainment", findViewById(R.id.entertainmentBudgetEditText));
        inputs.put("Education", findViewById(R.id.educationBudgetEditText));
        inputs.put("Savings", findViewById(R.id.savingsBudgetEditText));
        inputs.put("Others", findViewById(R.id.othersBudgetEditText));
        return inputs;
    }

    private String getCurrentMonthLabel() {
        return getMonthLabel(currentMonth, currentYear);
    }

    private String getMonthLabel(int month, int year) {
        String monthName = new DateFormatSymbols(Locale.getDefault()).getMonths()[month - 1];
        return monthName + " " + year;
    }

    private double parseAmount(String text) {
        if (text.isEmpty()) {
            return 0;
        }

        try {
            String sanitized = text.replaceAll("[^0-9,\\.]", "");
            int commaIndex = sanitized.lastIndexOf(',');
            int dotIndex = sanitized.lastIndexOf('.');
            if (commaIndex >= 0 && dotIndex >= 0) {
                if (commaIndex > dotIndex) {
                    sanitized = sanitized.replace(".", "");
                    sanitized = sanitized.replace(',', '.');
                } else {
                    sanitized = sanitized.replace(",", "");
                }
            } else if (commaIndex >= 0) {
                sanitized = sanitized.replace(',', '.');
            }
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String formatInputAmount(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private List<String> buildCategorySpinnerLabels(List<BudgetCategory> categories) {
        List<String> labels = new ArrayList<>();
        for (BudgetCategory category : categories) {
            labels.add(getCategoryIcon(category.getName()) + " " + category.getName() + " - remaining "
                    + CurrencyFormatter.format(category.getRemainingAmount()));
        }
        return labels;
    }

    private String getCategoryIcon(String categoryName) {
        if ("Food".equalsIgnoreCase(categoryName)) {
            return "\uD83C\uDF7D";
        }
        if ("Transport".equalsIgnoreCase(categoryName)) {
            return "\uD83D\uDE97";
        }
        if ("Bills".equalsIgnoreCase(categoryName)) {
            return "\uD83E\uDDFE";
        }
        if ("Entertainment".equalsIgnoreCase(categoryName)) {
            return "\uD83C\uDFAE";
        }
        if ("Education".equalsIgnoreCase(categoryName)) {
            return "\uD83C\uDF93";
        }
        if ("Savings".equalsIgnoreCase(categoryName)) {
            return "\uD83D\uDCB0";
        }
        return "\uD83D\uDCE6";
    }

    private void populateExpenseHistory(LinearLayout container, List<Expense> expenses) {
        container.removeAllViews();
        if (expenses.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.no_expenses_yet);
            emptyText.setTextColor(getColor(R.color.smart_text_secondary));
            emptyText.setTextSize(15);
            emptyText.setBackgroundResource(R.drawable.bg_info_panel);
            emptyText.setPadding(dp(14), dp(14), dp(14), dp(14));
            container.addView(emptyText);
            return;
        }

        for (Expense expense : expenses) {
            container.addView(createExpenseHistoryRow(expense));
        }
    }

    private void populateAdviceHistory(LinearLayout container, long budgetId) {
        container.removeAllViews();
        List<AiAdviceRecord> records = aiAdviceRepository.getAdviceHistory(budgetId);
        if (records.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.no_advice_yet);
            emptyText.setTextColor(getColor(R.color.smart_text_secondary));
            emptyText.setTextSize(15);
            emptyText.setBackgroundResource(R.drawable.bg_info_panel);
            emptyText.setPadding(dp(14), dp(14), dp(14), dp(14));
            container.addView(emptyText);
            return;
        }

        for (AiAdviceRecord record : records) {
            container.addView(createAdviceHistoryRow(record));
        }
    }

    private View createAdviceHistoryRow(AiAdviceRecord record) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_info_panel);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()
        );

        TextView titleText = new TextView(this);
        titleText.setText(dateFormat.format(new Date(record.getCreatedAt()))
                + " | HTTP " + record.getStatusCode());
        titleText.setTextColor(getColor(R.color.smart_text_primary));
        titleText.setTextSize(15);
        titleText.setTypeface(null, Typeface.BOLD);
        row.addView(titleText);

        TextView adviceText = new TextView(this);
        adviceText.setText(record.getAdviceText());
        adviceText.setTextColor(getColor(R.color.smart_text_secondary));
        adviceText.setTextSize(14);
        LinearLayout.LayoutParams adviceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        adviceParams.setMargins(0, dp(8), 0, 0);
        adviceText.setLayoutParams(adviceParams);
        row.addView(adviceText);

        return row;
    }

    private View createExpenseHistoryRow(Expense expense) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_info_panel);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        TextView titleText = new TextView(this);
        titleText.setText(getCategoryIcon(expense.getCategoryName()) + " "
                + expense.getTitle() + " - " + CurrencyFormatter.format(expense.getAmount()));
        titleText.setTextColor(getColor(R.color.smart_text_primary));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        row.addView(titleText);

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        String detail = dateFormat.format(new Date(expense.getExpenseDate()))
                + " | " + getCategoryLabel(expense.getCategoryName());
        if (!TextUtils.isEmpty(expense.getNote())) {
            detail = detail + "\n" + expense.getNote();
        }

        TextView detailText = new TextView(this);
        detailText.setText(detail);
        detailText.setTextColor(getColor(R.color.smart_text_secondary));
        detailText.setTextSize(14);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(4), 0, 0);
        detailText.setLayoutParams(detailParams);
        row.addView(detailText);

        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setText(R.string.delete);
        deleteButton.setTextColor(getColor(R.color.smart_error));
        deleteButton.setTextSize(14);
        deleteButton.setAllCaps(false);
        deleteButton.setStrokeColorResource(R.color.smart_error);
        deleteButton.setStrokeWidth(dp(1));
        deleteButton.setBackgroundColor(getColor(R.color.smart_surface));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        buttonParams.setMargins(0, dp(10), 0, 0);
        deleteButton.setLayoutParams(buttonParams);
        deleteButton.setOnClickListener(v -> {
            boolean deleted = expenseRepository.deleteExpense(expense.getId());
            showMessage(deleted ? "Expense deleted." : "Unable to delete expense.");
            showExpenseHistoryScreenWithFilter();
        });
        row.addView(deleteButton);

        return row;
    }

    private void renderAiChatConversation(LinearLayout container, ScrollView scrollView) {
        container.removeAllViews();

        if (aiChatMessages.isEmpty()) {
            container.addView(createAssistantMessageRow(getString(R.string.advice_placeholder)));
        } else {
            for (AiChatMessage message : aiChatMessages) {
                container.addView(message.isUser
                        ? createUserMessageRow(message.text)
                        : createAssistantMessageRow(message.text));
            }
        }

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void replaceLastAssistantMessage(String text) {
        for (int i = aiChatMessages.size() - 1; i >= 0; i--) {
            AiChatMessage message = aiChatMessages.get(i);
            if (!message.isUser) {
                aiChatMessages.set(i, new AiChatMessage(false, text));
                return;
            }
        }
        aiChatMessages.add(new AiChatMessage(false, text));
    }

    private View createUserMessageRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(16), 0, 0);
        row.setLayoutParams(rowParams);

        TextView messageText = new TextView(this);
        messageText.setText(text);
        messageText.setTextColor(getColor(R.color.white));
        messageText.setTextSize(15);
        messageText.setBackgroundResource(R.drawable.bg_ai_user_bubble);
        messageText.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(dp(56), 0, dp(4), 0);
        messageText.setLayoutParams(textParams);
        row.addView(messageText);
        return row;
    }

    private View createAssistantMessageRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.START | android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(16), 0, 0);
        row.setLayoutParams(rowParams);

        TextView avatarText = new TextView(this);
        avatarText.setText("AI");
        avatarText.setTextColor(getColor(R.color.white));
        avatarText.setTextSize(11);
        avatarText.setTypeface(null, Typeface.BOLD);
        avatarText.setGravity(android.view.Gravity.CENTER);
        avatarText.setBackgroundResource(R.drawable.bg_ai_avatar);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        avatarText.setLayoutParams(avatarParams);
        row.addView(avatarText);

        TextView messageText = new TextView(this);
        messageText.setText(text);
        messageText.setTextColor(getColor(R.color.smart_text_primary));
        messageText.setTextSize(16);
        messageText.setLineSpacing(0f, 1.15f);
        messageText.setTextIsSelectable(true);
        messageText.setBackgroundResource(R.drawable.bg_ai_bot_bubble);
        messageText.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.setMargins(dp(10), 0, 0, 0);
        messageText.setLayoutParams(textParams);
        row.addView(messageText);
        return row;
    }

    private static class AiChatMessage {
        private final boolean isUser;
        private final String text;

        private AiChatMessage(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }
    }
    private List<Expense> getFilteredExpenses(long userId) {
        if (historyDateStartMillis != null && historyDateEndMillis != null) {
            return expenseRepository.getExpensesForUserDate(
                    userId,
                    historyDateStartMillis,
                    historyDateEndMillis
            );
        }

        return expenseRepository.getExpensesForUserMonth(userId, historyMonth, historyYear);
    }

    private String buildExpenseFilterStatus() {
        if (historyDateStartMillis != null) {
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
            return "Showing expenses on " + dateFormat.format(new Date(historyDateStartMillis));
        }
        return "Showing expenses for " + getMonthLabel(historyMonth, historyYear);
    }

    private void shiftHistoryMonth(int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, historyYear);
        calendar.set(Calendar.MONTH, historyMonth - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, offset);
        historyMonth = calendar.get(Calendar.MONTH) + 1;
        historyYear = calendar.get(Calendar.YEAR);
    }

    private void showExpenseDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, historyYear);
        calendar.set(Calendar.MONTH, historyMonth - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(Calendar.YEAR, year);
                    start.set(Calendar.MONTH, monthOfYear);
                    start.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    start.set(Calendar.HOUR_OF_DAY, 0);
                    start.set(Calendar.MINUTE, 0);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);

                    Calendar end = (Calendar) start.clone();
                    end.add(Calendar.DAY_OF_MONTH, 1);

                    historyMonth = monthOfYear + 1;
                    historyYear = year;
                    historyDateStartMillis = start.getTimeInMillis();
                    historyDateEndMillis = end.getTimeInMillis();
                    showExpenseHistoryScreenWithFilter();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private CharSequence buildStyledReportOverview(BudgetSummary budgetSummary) {
        double allocated = budgetSummary.getTotalAllocated();
        double spent = budgetSummary.getTotalSpent();
        double remainingAllocated = allocated - spent;
        int spentPercent = allocated <= 0 ? 0 : (int) Math.round((spent / allocated) * 100);
        int overBudgetCount = 0;

        for (BudgetCategory category : budgetSummary.getCategories()) {
            if (category.getRemainingAmount() < 0) {
                overBudgetCount++;
            }
        }

        String incomeAmount = CurrencyFormatter.format(budgetSummary.getIncome());
        String allocatedAmount = CurrencyFormatter.format(allocated);
        String spentAmount = CurrencyFormatter.format(spent);
        String remainingAmount = CurrencyFormatter.format(remainingAllocated);
        String usedPercent = spentPercent + "%";
        String overBudgetText = String.valueOf(overBudgetCount);
        String text = "Income: " + incomeAmount
                + "\nAllocated: " + allocatedAmount
                + "\nSpent: " + spentAmount
                + "\nRemaining allocated budget: " + remainingAmount
                + "\nBudget used: " + usedPercent
                + "\nCategories over budget: " + overBudgetText;

        SpannableString styledText = new SpannableString(text);
        applySpan(styledText, incomeAmount, getColor(R.color.smart_primary), true);
        applySpan(styledText, allocatedAmount, getColor(R.color.smart_primary), true);
        applySpan(styledText, spentAmount, getCategoryPercentColor(spentPercent, spentPercent > 100), true);
        applySpan(styledText, usedPercent, getCategoryPercentColor(spentPercent, spentPercent > 100), true);
        applySpan(styledText, remainingAmount,
                remainingAllocated < 0 ? getColor(R.color.smart_error) : getColor(R.color.smart_primary), true);
        if (overBudgetCount > 0) {
            applySpan(styledText, "over budget", getColor(R.color.smart_error), true);
        }
        return styledText;
    }

    private CharSequence buildStyledBudgetHealthScore(BudgetSummary budgetSummary) {
        int score = 100;
        int overBudgetCount = 0;
        boolean hasSavings = false;

        for (BudgetCategory category : budgetSummary.getCategories()) {
            if (category.getRemainingAmount() < 0) {
                overBudgetCount++;
                score -= 12;
            }

            if ("Savings".equalsIgnoreCase(category.getName()) && category.getAllocatedAmount() > 0) {
                hasSavings = true;
            }
        }

        double allocated = budgetSummary.getTotalAllocated();
        double spentRate = allocated <= 0 ? 0 : budgetSummary.getTotalSpent() / allocated;
        if (spentRate > 1) {
            score -= 20;
        } else if (spentRate >= 0.8) {
            score -= 10;
        }

        if (budgetSummary.getUnallocatedAmount() < 0) {
            score -= 10;
        }

        if (hasSavings) {
            score += 5;
        }

        score = Math.max(0, Math.min(100, score));
        String status;
        if (score >= 85) {
            status = "Excellent";
        } else if (score >= 70) {
            status = "Good";
        } else if (score >= 50) {
            status = "Needs attention";
        } else {
            status = "At risk";
        }

        String scoreText = score + "/100";
        String text = "Budget Health: " + scoreText + " - " + status
                + "\nOver-budget categories: " + overBudgetCount;
        SpannableString styledText = new SpannableString(text);
        applySpan(styledText, "Budget Health", getColor(R.color.smart_primary), true);
        applySpan(styledText, scoreText, getHealthScoreColor(score), true);
        if (overBudgetCount > 0) {
            applySpan(styledText, "Over-budget", getColor(R.color.smart_error), true);
        }
        return styledText;
    }

    private CharSequence buildStyledOverspendingAlerts(BudgetSummary budgetSummary) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (BudgetCategory category : budgetSummary.getCategories()) {
            if (category.getRemainingAmount() < 0) {
                if (builder.length() == 0) {
                    appendStyled(builder, "Overspending Alerts", getColor(R.color.smart_text_primary), true);
                }
                String amount = CurrencyFormatter.format(Math.abs(category.getRemainingAmount()));
                builder.append("\n")
                        .append(getCategoryLabel(category.getName()))
                        .append(" is ");
                appendStyled(builder, "over budget", getColor(R.color.smart_error), true);
                builder.append(" by ");
                appendStyled(builder, amount, getColor(R.color.smart_error), true);
            } else if (category.getAllocatedAmount() > 0) {
                double usedRate = category.getSpentAmount() / category.getAllocatedAmount();
                if (usedRate >= 0.9) {
                    if (builder.length() == 0) {
                        appendStyled(builder, "Overspending Alerts", getColor(R.color.smart_text_primary), true);
                    }
                    String percent = Math.round(usedRate * 100) + "%";
                    builder.append("\n")
                            .append(getCategoryLabel(category.getName()))
                            .append(" has used ");
                    appendStyled(builder, percent, getColor(R.color.smart_accent), true);
                    builder.append(" of its budget.");
                }
            }
        }
        return builder;
    }

    private void populateCategoryReport(LinearLayout container, List<BudgetCategory> categories) {
        container.removeAllViews();
        for (BudgetCategory category : categories) {
            container.addView(createCategoryReportRow(category));
        }
    }

    private void populateSpendingChart(
            SpendingPieChartView chartView,
            LinearLayout legendContainer,
            List<BudgetCategory> categories
    ) {
        legendContainer.removeAllViews();
        List<SpendingPieChartView.Slice> slices = new ArrayList<>();
        int[] colors = getSpendingChartColors();
        double totalSpent = 0;
        int colorIndex = 0;

        for (BudgetCategory category : categories) {
            double spentAmount = category.getSpentAmount();
            if (spentAmount <= 0) {
                continue;
            }

            int color = colors[colorIndex % colors.length];
            slices.add(new SpendingPieChartView.Slice(spentAmount, color));
            legendContainer.addView(createSpendingLegendRow(category, color));
            totalSpent += spentAmount;
            colorIndex++;
        }

        if (slices.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No spending recorded for this month yet.");
            emptyText.setTextColor(getColor(R.color.smart_text_secondary));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, dp(4), 0, 0);
            legendContainer.addView(emptyText);
        }

        chartView.setSlices(slices, CurrencyFormatter.format(totalSpent));
    }

    private View createSpendingLegendRow(BudgetCategory category, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

        View swatch = new View(this);
        GradientDrawable swatchBackground = new GradientDrawable();
        swatchBackground.setColor(color);
        swatchBackground.setCornerRadius(dp(3));
        swatch.setBackground(swatchBackground);
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(14), dp(14));
        swatchParams.setMargins(0, dp(3), dp(10), 0);
        swatch.setLayoutParams(swatchParams);
        row.addView(swatch);

        TextView labelText = new TextView(this);
        labelText.setText(getCategoryLabel(category.getName())
                + " - " + CurrencyFormatter.format(category.getSpentAmount()));
        labelText.setTextColor(getColor(R.color.smart_text_primary));
        labelText.setTextSize(14);
        labelText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.addView(labelText);

        return row;
    }

    private int[] getSpendingChartColors() {
        return new int[]{
                getColor(R.color.smart_primary),
                getColor(R.color.smart_accent),
                Color.rgb(70, 130, 180),
                Color.rgb(132, 94, 194),
                Color.rgb(219, 111, 83),
                Color.rgb(88, 154, 92),
                Color.rgb(120, 132, 145)
        };
    }

    private View createCategoryReportRow(BudgetCategory category) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(category.getRemainingAmount() < 0
                ? R.drawable.bg_warning_panel
                : R.drawable.bg_info_panel);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        TextView titleText = new TextView(this);
        titleText.setText(getCategoryLabel(category.getName()));
        titleText.setTextColor(getColor(R.color.smart_text_primary));
        titleText.setTextSize(16);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(titleText);

        TextView detailText = new TextView(this);
        detailText.setText(buildStyledCategoryReportDetail(category));
        detailText.setTextColor(getColor(R.color.smart_text_secondary));
        detailText.setTextSize(14);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(4), 0, 0);
        detailText.setLayoutParams(detailParams);
        row.addView(detailText);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(getCategoryUsedPercent(category));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
        );
        progressParams.setMargins(0, dp(10), 0, 0);
        progressBar.setLayoutParams(progressParams);
        row.addView(progressBar);

        return row;
    }

    private CharSequence buildStyledCategoryReportDetail(BudgetCategory category) {
        int percent = getCategoryUsedPercent(category);
        String spentAmount = CurrencyFormatter.format(category.getSpentAmount());
        String allocatedAmount = CurrencyFormatter.format(category.getAllocatedAmount());
        String percentText = percent + "%";
        String detail = spentAmount
                + " spent of "
                + allocatedAmount
                + " (" + percentText + ")";
        SpannableStringBuilder builder = new SpannableStringBuilder(detail);
        appendStyledAmount(builder, spentAmount, category.getRemainingAmount() < 0);
        applySpan(builder, percentText, getCategoryPercentColor(percent, category.getRemainingAmount() < 0), true);

        if (category.getRemainingAmount() < 0) {
            String overAmount = CurrencyFormatter.format(Math.abs(category.getRemainingAmount()));
            builder.append("\n");
            appendStyled(builder, "Over budget", getColor(R.color.smart_error), true);
            builder.append(" by ");
            appendStyled(builder, overAmount, getColor(R.color.smart_error), true);
            return builder;
        }

        String remainingAmount = CurrencyFormatter.format(category.getRemainingAmount());
        builder.append("\nRemaining ");
        appendStyled(builder, remainingAmount, getColor(R.color.smart_primary), true);
        return builder;
    }

    private void appendStyledAmount(SpannableStringBuilder builder, String amount, boolean warning) {
        applySpan(builder, amount, warning ? getColor(R.color.smart_error) : getColor(R.color.smart_primary), true);
    }

    private void appendStyled(SpannableStringBuilder builder, String text, int color, boolean bold) {
        int start = builder.length();
        builder.append(text);
        builder.setSpan(new ForegroundColorSpan(color), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (bold) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applySpan(Spannable styledText, String target, int color, boolean bold) {
        String fullText = styledText.toString();
        int start = fullText.indexOf(target);
        if (start < 0) {
            return;
        }

        int end = start + target.length();
        styledText.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (bold) {
            styledText.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private int getHealthScoreColor(int score) {
        if (score >= 70) {
            return getColor(R.color.smart_primary);
        }
        if (score >= 50) {
            return getColor(R.color.smart_accent);
        }
        return getColor(R.color.smart_error);
    }

    private int getCategoryPercentColor(int percent, boolean isOverBudget) {
        if (isOverBudget || percent >= 100) {
            return getColor(R.color.smart_error);
        }
        if (percent >= 90) {
            return getColor(R.color.smart_accent);
        }
        return getColor(R.color.smart_primary);
    }

    private String getCategoryLabel(String categoryName) {
        return getCategoryIcon(categoryName) + " " + categoryName;
    }

    private String getInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "S";
        }
        return name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private int getCategoryUsedPercent(BudgetCategory category) {
        if (category.getAllocatedAmount() <= 0) {
            return category.getSpentAmount() > 0 ? 100 : 0;
        }
        int percent = (int) Math.round((category.getSpentAmount() / category.getAllocatedAmount()) * 100);
        return Math.min(Math.max(percent, 0), 100);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}







