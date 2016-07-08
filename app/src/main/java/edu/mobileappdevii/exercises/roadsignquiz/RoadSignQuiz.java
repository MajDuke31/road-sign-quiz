package edu.mobileappdevii.exercises.roadsignquiz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager; //for accessing files in assets folder
import android.graphics.drawable.Drawable; //for displaying the image file in an ImageView after reading it in.
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler; //used to execute a Runnable object in the future
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;  //used for logging exceptions for debugging purposes – viewed by using the Android logcat tool and are also displayed in the Android DDMS (Dalvik Debug Monitor Server) perspective’s LogCat tab in Eclipse.
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem; //along with Menu class used to display a context menu.
import android.view.animation.Animation;  //used for the animations
import android.view.animation.AnimationUtils;  //used for the animations
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException; //for reading image files from assets folder
import java.io.InputStream; //for reading image files from assets folder
import java.util.ArrayList; //for holding image file names and current quiz items
import java.util.Collections; //for shuffle method
import java.util.HashMap; //storing sign type names and corresponding Boolean values indicating whether each sign type is enabled or disabled.
import java.util.Random;
import java.util.Set;

public class RoadSignQuiz extends AppCompatActivity {
    // String used when logging error messages
    private static final String TAG = "RoadSignQuizGame Activity";

    // Create constants for each menu id
    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int SIGN_TYPES_MENU_ID = Menu.FIRST + 1;

    private ArrayList<String> fileNameList; // road sign file names
    private ArrayList<String> quizSignsList; // names of signs in quiz
    private HashMap<String, Boolean> signTypesMap; // which sign types are enabled
    private int guessRows; // number of rows displaying choices
    private Random random; // random number generator
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess
    private TextView questionNumberTextView; // shows current question #
    private ImageView roadSignImageView; // displays a road sign
    private TableLayout buttonTableLayout; // table of answer Buttons
    private TextView answerTextView; // displays Correct! or Incorrect!
    private int correctAnswers; // number of correct guesses
    private int totalGuesses; // number of guesses made
    private String correctAnswer; // correct sign name for the current sign
    private GestureDetectorCompat simpleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_sign_quiz);
        simpleGestureDetector = new GestureDetectorCompat(this, new RoadSignGestureListener());

        fileNameList = new ArrayList<>(); // List of image file names
        quizSignsList = new ArrayList<>(); // Flags in this quiz
        signTypesMap = new HashMap<>(); // HashMap of regions
        guessRows = 1; // Default to 1 row of choices
        random = new Random();
        handler = new Handler(); // Used to perform delayed operations
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // Repeat animation 3 times

        // Dynamically get array of road sign types from strings.xml
        String[] signTypes = getResources().getStringArray(R.array.signTypesList);

        // By default, signs are chosen from all sign types
        for (String signType : signTypes) {
            signTypesMap.put(signType, true);
        }

        // Get references to GUI items
        questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        roadSignImageView = (ImageView) findViewById(R.id.roadSignImageView);
        buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView) findViewById(R.id.answerTextView);

        // Set the questionNumberTextView's text
        questionNumberTextView.setText("Question" +
                " 1 " + "of" + " 10");

        resetQuiz(); // Start a new quiz
    }

    private void resetQuiz() {
        // Use the AssetManager to get image road sign for enabled sign types
        AssetManager assets = getAssets(); // Get the app's AssetManager
        fileNameList.clear();

        try {
            Set<String> signTypes = signTypesMap.keySet(); // Get Set of sign types
            // Loop through each sign type
            for (String signType : signTypes) {
                if (signTypesMap.get(signType)) {
                    // If sign type is enabled, get a list of all signs for
                    // this sign type
                    signType = signType.replace(' ', '_');
                    String[] paths = assets.list(signType);
                    for (String path : paths) {
                        fileNameList.add(path.replace(".gif", ""));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }
        correctAnswers = 0; // Reset it
        totalGuesses = 0; // Reset it
        quizSignsList.clear(); // Clear prior questions

        // Add 10 random filenames to the list
        int signCounter = 1;
        int numberOfSigns = fileNameList.size();

        while (signCounter <= 10) {
            int randomIndex = random.nextInt(numberOfSigns);
            String fileName = fileNameList.get(randomIndex);
            // If the sign type is enabled and hasn't already been chosen
            if (!quizSignsList.contains(fileName)) {
                quizSignsList.add(fileName);
                signCounter++;
            }
        } // while
        loadNextSign(); // Start the quiz by loading 1st sign
    }

    // After the user guesses a correct sign, load the next sign
    private void loadNextSign() {
        // Get file name of the next sign and remove it from the list
        String nextImageName = quizSignsList.remove(0);
        correctAnswer = nextImageName; // Update the correct answer

        answerTextView.setText(""); // Clear answerTextView

        // Display the number of the current question in the quiz
        questionNumberTextView.setText(
                "Question" + " " +
                        (correctAnswers + 1) + " " +
                        "of" + " 10");

        // Extract the sign type from the next image's name
        String signType = nextImageName.substring(0, nextImageName.indexOf('-'));

        // Use AssetManager to load next image from assets folder
        AssetManager assets = getAssets(); // Get app's AssetManager
        InputStream stream; // Used to read in sign images

        try {
            // Get an InputStream to the asset representing the next sign
            stream = assets.open(signType + "/" + nextImageName + ".gif");

            // Load the assets as a Drawable and display on the signImageView
            Drawable sign = Drawable.createFromStream(stream, nextImageName);
            roadSignImageView.setImageDrawable(sign);
            roadSignImageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Add the SimpleOnGestureListener to the roadSignImageView
                    simpleGestureDetector.onTouchEvent(event);
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error loading " + nextImageName, e);
        }

        // Clear prior answer Buttons from TableRows
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();
        }
        Collections.shuffle(fileNameList); // Shuffle file names

        // Put the correct answer at the end of fileNameList later will be inserted randomly into the answer Buttons
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // Get a reference to the LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // Add 3, 6, or 9 answer Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            TableRow currentTableRow = getTableRow(row); // Obtains the TableRow at a specific index in the buttonTableLayout

            // Place Buttons in currentTableRow
            for (int column = 0; column < 3; column++) {
                // Inflate guess_button.xml to create new Button
                Button newGuessButton = (Button) inflater.inflate(R.layout.guess_button, null);

                // Get sign name and set it as newGuessButton's text
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getSignName(fileName));

                // Register answerButtonListener to respond to button clicks
                newGuessButton.setOnClickListener(guessButtonListener);
                currentTableRow.addView(newGuessButton);
            } // End for
        } // End for

        // Randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // Pick random row
        int column = random.nextInt(3); // Pick random column
        TableRow randomTableRow = getTableRow(row); // Get the TableRow
        String signName = getSignName(correctAnswer);
        ((Button) randomTableRow.getChildAt(column)).setText(signName);
    } // End method loadNextSign

    private String getSignName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    } // End method getSignName

    // Returns the specified TableRow
    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    } // End method getTableRow

    // Called when the user selects an answer
    private void submitGuess(Button guessButton) {
        String guess = guessButton.getText().toString();
        String answer = getSignName(correctAnswer);
        ++totalGuesses; // Increment the number of guesses the user has made

        // If the guess is correct
        if (guess.equals(answer)) {
            ++correctAnswers; // Increment the number of correct answers

            // Display "Correct!" in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

            disableButtons(); // Disable all answer Buttons

            // If the user has correctly identified 10 signs
            if (correctAnswers == 10) {
                // Create a new AlertDialg Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Reset Quiz"); // Title bar string

                // Set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, "guesses",
                        (1000 / (double) totalGuesses),
                        "correct"));

                builder.setCancelable(false);

                // Add "Reset Quiz" Button
                builder.setPositiveButton("Reset Quiz", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetQuiz();
                    } // end anonymous inner class
                }); // end call to setPositiveButton

                AlertDialog resetDialog = builder.create();
                resetDialog.show(); // Display the Dialog
            } // end if
            else { // Answer is correct but quiz is not over
                // Load the next sign after a 1-second delay
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNextSign();
                    }
                }, 1000); // 1000 milliseconds for 1-second delay
            } // end else
        } // end if
        else { // Guess was incorrect
            // Play the animation
            roadSignImageView.startAnimation(shakeAnimation);

            // Display "Incorrect!" in red
            answerTextView.setText("Incorrect!");
            answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // Disable the incorrect answer
        } // end else
    } // end method submitGuess

    // Utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < buttonTableLayout.getChildCount(); row++) {
            TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); i++) {
                tableRow.getChildAt(i).setEnabled(false);
            }
        } // end outer for
    } // end method disableButtons

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Add 2 options to the menu
        // 1st argument is menuitem's group id
        // 2nd argument is item's unique id
        // 3rd argument is the order it appears
        // 4th argument is what is displayed
        menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, "Select Number of Choices");
        menu.add(Menu.NONE, SIGN_TYPES_MENU_ID, Menu.NONE, "Select Sign Types");

        return true;
    }

    // Called when the user selects an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Switch the menu id of the user-selected option
        switch (item.getItemId()) { // Gets unique ID of the item
            case CHOICES_MENU_ID:
                // Create a list of the possible numbers of answer choices
                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);

                // Create a new AlertDialog Builder and set its title
                AlertDialog.Builder choicesBuilder = new AlertDialog.Builder(this);
                choicesBuilder.setTitle("Select Number of Choices");

                // Add possibleChoices's items to the Dialog and set the
                // behavior when one of the items is clicked
                choicesBuilder.setItems(R.array.guessesList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Update guessRows to match the user's choice
                        guessRows = Integer.parseInt(
                                possibleChoices[which]) / 3;
                        resetQuiz();
                    }
                });

                // Create an AlertDialog from the Builder
                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show(); // Show the Dialog
                return true;

            case SIGN_TYPES_MENU_ID:
                // Get array of sign types
                final String[] signTypeNames =
                        signTypesMap.keySet().toArray(new String[signTypesMap.size()]);

                // Boolean array representing whether each sign type is enabled
                boolean[] signTypesEnabled = new boolean[signTypesMap.size()];
                for (int i = 0; i < signTypesEnabled.length; i++) {
                    signTypesEnabled[i] = signTypesMap.get(signTypeNames[i]);
                }

                // Create an AlertDialog Builder and set the Dialog's title
                AlertDialog.Builder signTypesBuilder = new AlertDialog.Builder(this);
                signTypesBuilder.setTitle("Select Sign Types");

                // Replace _ with space in sign type names for display purposes
                String[] displayNames = new String[signTypeNames.length];
                for (int i = 0; i < signTypeNames.length; i++) {
                    displayNames[i] = signTypeNames[i].replace('_', ' ');
                }

                // Add displayNames to the Dialog and set the behavior
                // when one of the items is clicked
                signTypesBuilder.setMultiChoiceItems(displayNames, signTypesEnabled,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                // Include or exclude the clicked sign type
                                // depending on whether or not it's checked
                                signTypesMap.put(signTypeNames[which], isChecked);
                            }
                        });

                // Resets quiz when user presses the "Reset Quiz" Button
                signTypesBuilder.setPositiveButton("Reset Quiz", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetQuiz(); // Reset the quiz
                    }
                });
                AlertDialog signTypesDialog = signTypesBuilder.create();
                signTypesDialog.show(); // Display the Dialog
                return true;
        } // end switch

        return super.onOptionsItemSelected(item);
    } // end method onOptionsItemSelected

    // Called when a guess Button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            submitGuess((Button)v); // Pass selected button to submitGuess()
        }
    };

    // This class extends the SimpleOnGestureListener class to handle double tap gesture events.
    class RoadSignGestureListener extends SimpleOnGestureListener {
        // Holds the URL of the website containing all of the road signs used in the app
        private static final String URL = "http://www.trafficsign.us";

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Create an intent to open up a web page in the device's web browser
            Intent openWebPageIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));

            // Verify that the intent will resolve to an activity
            if (openWebPageIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(openWebPageIntent);
            }
            return true;
        }
    }
}
