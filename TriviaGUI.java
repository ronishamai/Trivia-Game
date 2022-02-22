package il.ac.tau.cs.sw1.trivia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TriviaGUI {

	private static final int MAX_ERRORS = 3;
	private Shell shell;
	private Label scoreLabel;
	private Composite questionPanel;
	private Label startupMessageLabel;
	private Font boldFont;
	private String lastAnswer;

	// Currently visible UI elements.
	Label instructionLabel;
	Label questionLabel;
	private List<Button> answerButtons = new LinkedList<>();
	private Button passButton;
	private Button fiftyFiftyButton;

	private Map<Question, Boolean> questions = new HashMap<Question, Boolean>();
	private int score;
	private Question currentQuestion;
	private int currentQuestionNum;
	private int errors;
	private PassButtonListener passButtonListener;
	private FiftyFiftyButtonListener fiftyFiftyButtonListener;

	public void open() {
		createShell();
		runApplication();
	}

	/**
	 * Creates the widgets of the application main window
	 */
	private void createShell() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Trivia");

		// window style
		Rectangle monitor_bounds = shell.getMonitor().getBounds();
		shell.setSize(new Point(monitor_bounds.width / 3, monitor_bounds.height / 4));
		shell.setLayout(new GridLayout());

		FontData fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(shell.getDisplay(), fontData);

		// create window panels
		createFileLoadingPanel();
		createScorePanel();
		createQuestionPanel();
	}

	/**
	 * Creates the widgets of the form for trivia file selection
	 */
	private void createFileLoadingPanel() {
		final Composite fileSelection = new Composite(shell, SWT.NULL);
		fileSelection.setLayoutData(GUIUtils.createFillGridData(1));
		fileSelection.setLayout(new GridLayout(4, false));

		final Label label = new Label(fileSelection, SWT.NONE);
		label.setText("Enter trivia file path: ");

		// text field to enter the file path
		final Text filePathField = new Text(fileSelection, SWT.SINGLE | SWT.BORDER);
		filePathField.setLayoutData(GUIUtils.createFillGridData(1));

		// "Browse" button
		final Button browseButton = new Button(fileSelection, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String path = GUIUtils.getFilePathFromFileDialog(shell);
				if (path != null) {
					filePathField.setText(path);
				}
			}
		});

		// "Play!" button
		final Button playButton = new Button(fileSelection, SWT.PUSH);
		playButton.setText("Play!");
		playButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String path = filePathField.getText();
				if (path == "" || path == null) {
					// Error...
					return;
				}

				initFields();

				try {
					BufferedReader reader = new BufferedReader(new FileReader(path));
					String line;
					while ((line = reader.readLine()) != null) {
						questions.putIfAbsent(new Question(line), false);
					}

					nextQuestion();

					reader.close();
				} catch (IOException ex) {
					GUIUtils.showErrorDialog(shell, ex.getMessage());
				}
			}
		});
	}

	private void initFields() {
		this.score = 0;
		this.scoreLabel.setText(String.valueOf(this.score));
		this.lastAnswer = "";
		this.questions.clear();
		this.currentQuestion = null;
		this.currentQuestionNum = 0;
		this.errors = 0;
		this.passButtonListener = new PassButtonListener();
		this.fiftyFiftyButtonListener = new FiftyFiftyButtonListener();
	}

	private void nextQuestion() {
		if (this.errors >= MAX_ERRORS) {
			GUIUtils.showInfoDialog(shell, "GAME OVER",
					String.format("Your final score is %d after %s questions.", this.score, this.currentQuestionNum));
			return;
		}

		List<Question> possibleQs = this.questions.keySet().stream().filter(q -> this.questions.get(q) == false)
				.collect(Collectors.toList());

		Question question = possibleQs.get((new Random()).nextInt(possibleQs.size() - 1));
		this.questions.put(question, true);
		this.currentQuestion = question;
		this.currentQuestionNum++;

		List<String> answers = new ArrayList<String>(question.getAnsweres());
		Collections.shuffle(answers);

		updateQuestionPanel(question.getQuestion(), answers);
	}

	/**
	 * Creates the panel that displays the current score
	 */
	private void createScorePanel() {
		Composite scorePanel = new Composite(shell, SWT.BORDER);
		scorePanel.setLayoutData(GUIUtils.createFillGridData(1));
		scorePanel.setLayout(new GridLayout(2, false));

		final Label label = new Label(scorePanel, SWT.NONE);
		label.setText("Total score: ");

		// The label which displays the score; initially empty
		scoreLabel = new Label(scorePanel, SWT.NONE);
		scoreLabel.setLayoutData(GUIUtils.createFillGridData(1));
	}

	/**
	 * Creates the panel that displays the questions, as soon as the game starts.
	 * See the updateQuestionPanel for creating the question and answer buttons
	 */
	private void createQuestionPanel() {
		questionPanel = new Composite(shell, SWT.BORDER);
		questionPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		questionPanel.setLayout(new GridLayout(2, true));

		// Initially, only displays a message
		startupMessageLabel = new Label(questionPanel, SWT.NONE);
		startupMessageLabel.setText("No question to display, yet.");
		startupMessageLabel.setLayoutData(GUIUtils.createFillGridData(2));
	}

	/**
	 * Serves to display the question and answer buttons
	 */
	private void updateQuestionPanel(String question, List<String> answers) {
		// clear the question panel
		Control[] children = questionPanel.getChildren();
		for (Control control : children) {
			control.dispose();
		}

		// create the instruction label
		instructionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		instructionLabel.setText(lastAnswer + "Answer the following question:");
		instructionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the question label
		questionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		questionLabel.setText(question);
		questionLabel.setFont(boldFont);
		questionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the answer buttons
		answerButtons.clear();
		for (int i = 0; i < 4; i++) {
			Button answerButton = new Button(questionPanel, SWT.PUSH | SWT.WRAP);
			answerButton.setText(answers.get(i));
			GridData answerLayoutData = GUIUtils.createFillGridData(1);
			answerLayoutData.verticalAlignment = SWT.FILL;
			answerButton.setLayoutData(answerLayoutData);

			answerButton.addSelectionListener(new AnswerButtonListener());

			answerButtons.add(answerButton);
		}

		// create the "Pass" button to skip a question
		passButton = new Button(questionPanel, SWT.PUSH);
		passButton.setText("Pass");
		GridData data = new GridData(GridData.END, GridData.CENTER, true, false);
		data.horizontalSpan = 1;
		passButton.setLayoutData(data);
		passButton.addSelectionListener(passButtonListener);
		passButtonListener.setEnabled();

		// create the "50-50" button to show fewer answer options
		fiftyFiftyButton = new Button(questionPanel, SWT.PUSH);
		fiftyFiftyButton.setText("50-50");
		data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		data.horizontalSpan = 1;
		fiftyFiftyButton.setLayoutData(data);
		fiftyFiftyButton.addSelectionListener(fiftyFiftyButtonListener);
		fiftyFiftyButtonListener.setEnabled();

		// two operations to make the new widgets display properly
		questionPanel.pack();
		questionPanel.getParent().layout();
	}

	private class AnswerButtonListener implements SelectionListener {
		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.getSource() instanceof Button) {
				Button b = (Button) e.getSource();

				if (currentQuestion.isCorrectAnswer(b.getText())) {
					score += 3;
				} else {
					score -= 2;
					errors += 1;
				}
				scoreLabel.setText(String.valueOf(score));

				nextQuestion();
			}
		}
	}

	private class PassButtonListener implements SelectionListener {
		private int clicks = 0;

		@Override
		public void widgetSelected(SelectionEvent e) {
			clicks++;
			if (clicks > 1) {
				score--;
				scoreLabel.setText(String.valueOf(score));
			}
			currentQuestionNum--; // skipped
			nextQuestion();
		}

		public void setEnabled() {
			if ((clicks != 0) && (score <= 0)) {
				if (passButton != null) {
					passButton.setEnabled(false);
				}
			} else {
				if (passButton != null) {
					passButton.setEnabled(true);
				}
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) {
			// TODO Auto-generated method stub

		}
	}

	private class FiftyFiftyButtonListener implements SelectionListener {
		private int clicks = 0;

		@Override
		public void widgetSelected(SelectionEvent e) {
			clicks++;
			if (clicks > 1) {
				score--;
				scoreLabel.setText(String.valueOf(score));
			}

			int buttonsToDisable = 2;
			while (buttonsToDisable > 0) {
				Button b = answerButtons.get((new Random()).nextInt(3));
				if (!b.getEnabled()) {
					continue;
				}
				if (currentQuestion.isCorrectAnswer(b.getText())) {
					continue;
				}
				b.setEnabled(false);
				buttonsToDisable--;
			}

			fiftyFiftyButton.setEnabled(false);
		}

		public void setEnabled() {
			if ((clicks != 0) && (score <= 0)) {
				if (fiftyFiftyButton != null) {
					fiftyFiftyButton.setEnabled(false);
				}
			} else {
				if (fiftyFiftyButton != null) {
					fiftyFiftyButton.setEnabled(true);
				}
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent arg0) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Opens the main window and executes the event loop of the application
	 */
	private void runApplication() {
		shell.open();
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		boldFont.dispose();
	}
}
