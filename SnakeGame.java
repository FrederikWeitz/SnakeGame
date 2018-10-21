import java.net.URL;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SnakeGame extends Application{
	
	// Variablen
	public enum Direction { 
		UP, DOWN, LEFT, RIGHT;
	}
	public enum Speed {
		SLOW(0.7), MIDDLE(0.5), FAST(0.3);
		private Speed(double speed) {
			this.speed = speed;
		}
		private double speed;
		public double get() {return speed;}
	}
	
	// Fenster
	private static Stage window;
	
	public static final int BLOCK_SIZE = 20;
	public static final int GAME_WIDTH = 30 * BLOCK_SIZE;
	public static final int GAME_HEIGHT = 20 * BLOCK_SIZE;
	
	private Speed speed = Speed.SLOW;
	private static boolean isEndless = false;
	
	// Variables for Snake
	private Direction direction = Direction.RIGHT;
	private boolean moved = false;
	private boolean running = false;
	
	private Timeline timeline = new Timeline();
	
	private ObservableList<Node> snake;
	
	// Variables for MediaPlayer
	private MediaPlayer mediaPlayer;
	private boolean isMute = false;
	private Slider volumeSlider = new Slider();
	private Label volumeLabel = new Label("1.0");
	
	// Variables for Score Label
	private int score = 0;
	private Label scoreLabel = new Label("Score: " + score);
	private Label infoLabel = new Label("Drücke ESC für Exit und SPACE für Pause!");
	
	// Variables for Sound
	AudioClip hitFoodSound;
	AudioClip failSound;
	
	@Override
	public void init() {
		String musicFile = "/musicandsound/snakemusic.wav";
		playMusic(musicFile);
		
		// EventSounds
		URL fileUrl = getClass().getResource("/musicandsound/hit.wav");
		hitFoodSound = new AudioClip(fileUrl.toString());
		
		fileUrl = getClass().getResource("/musicandsound/fail.wav");
		failSound = new AudioClip(fileUrl.toString());
		
		
		volumeSlider.setValue(mediaPlayer.getVolume());
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = createStartScreen();
		
		primaryStage.setResizable(false);
		primaryStage.setTitle("Snake");
		window = primaryStage;
		
		window.setScene(new Scene(root, GAME_WIDTH, GAME_HEIGHT));
		
		window.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	
	private Pane createGameContent() {
		Pane root = new Pane();
		root.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
		
		if (isEndless) {
			root.setStyle("" 
					+ "-fx-background-color: greenyellow;"
					);
		} else {
			root.setStyle("" 
					+ "-fx-background-color: greenyellow;"
					+ "-fx-border-color: darkgreen;"
					+ "-fx-border-style: solid;"
					+ "-fx-border-width: 2;");
		}
		
		// Schlange
		Group snakeBody = new Group();
		snake = snakeBody.getChildren();
		
		// Essen
		ImageView food = new ImageView(new Image("/images/apple.png"));
		food.setFitHeight(BLOCK_SIZE);
		food.setFitWidth(BLOCK_SIZE);
		
		createRandomFood(food);
		
		// Animation
		KeyFrame keyFrame = new KeyFrame(Duration.seconds(speed.get()), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(!running) {
					return;
				}
				boolean toRemove = snake.size()>1;
				
				Node tail; // Ende der Schlange
				if (toRemove) {
					tail = snake.remove(snake.size() - 1);
				} else {
					tail = snake.get(0);
				}
				
				switch (direction) {
				case UP:
					tail.setTranslateX(snake.get(0).getTranslateX());
					tail.setTranslateY(snake.get(0).getTranslateY() - BLOCK_SIZE);
					break;
				case DOWN:
					tail.setTranslateX(snake.get(0).getTranslateX());
					tail.setTranslateY(snake.get(0).getTranslateY() + BLOCK_SIZE);
					break;
				case LEFT:
					tail.setTranslateX(snake.get(0).getTranslateX() - BLOCK_SIZE);
					tail.setTranslateY(snake.get(0).getTranslateY());
					break;
				case RIGHT:
					tail.setTranslateX(snake.get(0).getTranslateX() + BLOCK_SIZE);
					tail.setTranslateY(snake.get(0).getTranslateY());
					break;
				default: break;
				}
				
				moved = true;
				if (toRemove) {
					snake.add(0, tail);
				}
				
				// Kollision mit sich selbst
				for(Node rect: snake) {
					if(rect != tail && tail.getTranslateX() == rect.getTranslateX()
							&& tail.getTranslateY() == rect.getTranslateY()) {
						failSound.play();
						score = 0;
						scoreLabel.setText("Score: " + score);
						restartGame();
						break;
					}
				}
				
				// Wand oder nicht?
				if (isEndless) {
					gameIsEndless(tail, root);
				} else {
					gameIsNotEndless(tail, food);
				}
				
				// Essen einsammeln
				if (tail.getTranslateX() == food.getTranslateX() &&
						tail.getTranslateY() == food.getTranslateY()) {
					score++;
					scoreLabel.setText("Score: " + score);
					createRandomFood(food);
					hitFoodSound.play();
					
					snake.add(createSnakePart(false));
				}
			}
		});
		
		timeline.getKeyFrames().add(keyFrame);
		timeline.setCycleCount(Timeline.INDEFINITE);
		
		// ScoreLabel
		scoreLabel.setFont(Font.font("Arial", 30));
		scoreLabel.setTranslateX(GAME_WIDTH / 2);
		
		// InfoLabel
		infoLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 10));
		
		
		// Elemente hinzufügen
		root.getChildren().addAll(food,snakeBody,scoreLabel,infoLabel);
		
		return root;
	}
	
	
	// Random Food Spawn
	private void createRandomFood(Node food) {
		boolean isUnderSnake = true;
		while(isUnderSnake) {
			food.setTranslateX((int) (Math.random() * (GAME_WIDTH / BLOCK_SIZE)) * BLOCK_SIZE);
			food.setTranslateY((int) (Math.random() * (GAME_HEIGHT / BLOCK_SIZE)) * BLOCK_SIZE);
			boolean tempUnderSnake = false;
			for(Node part: snake) {
				if(part.getTranslateX()==food.getTranslateX() && part.getTranslateY()==food.getTranslateY()) {
					tempUnderSnake = true;
				}
			}
			isUnderSnake = tempUnderSnake;
		}
	}
	
	// isEndless
	private void gameIsEndless(Node tail, Parent root) {
		if (tail.getTranslateX()<0) {
			tail.setTranslateX(GAME_WIDTH - BLOCK_SIZE);
		}
		
		if (tail.getTranslateX()>=GAME_WIDTH) {
			tail.setTranslateX(0);
		}
		
		if (tail.getTranslateY()<0) {
			tail.setTranslateY(GAME_HEIGHT - BLOCK_SIZE);
		}
		
		if (tail.getTranslateY()>=GAME_HEIGHT) {
			tail.setTranslateY(0);
		}
	}
	
	// isNotEndless
	private void gameIsNotEndless(Node tail, Node food) {		
		if (tail.getTranslateX()<0 || tail.getTranslateX()>GAME_WIDTH
				|| tail.getTranslateY()<0 || tail.getTranslateY()>GAME_HEIGHT) {
			failSound.play();
			score = 0;
			scoreLabel.setText("Score: " + score);
			restartGame();
			createRandomFood(food);
		}
	}
	
	// Start Game
	private void startGame() {
		snake.add(createSnakePart(true));
		
		direction=Direction.RIGHT;
		timeline.play();
		running = true;
	}
	
	// Restart Game
	private void restartGame() {
		stopGame();
		startGame();
	}
	
	
	// Stopp Game
	private void stopGame() {
		running = false;
		timeline.stop();
		snake.clear();
	}
	
	// create Snake Part
	private Circle createSnakePart(boolean isFirst) {
		Circle circle = new Circle();
		circle.setRadius(BLOCK_SIZE/2);
		circle.setCenterX(BLOCK_SIZE/2);
		circle.setCenterY(BLOCK_SIZE/2);
		circle.setFill(Color.GREEN);
		
		if(!isFirst) {
			circle.setTranslateX(-BLOCK_SIZE);
		}
		
		return circle;
	}
	
	// Tastatur Interaktion
	private void keypressed(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (!moved) {
					return;
				}
				switch(event.getCode()) {
				case W:
				case UP:
					if(direction != Direction.DOWN) {
						direction = Direction.UP;
						break;
					}
				case S:
				case DOWN:
					if(direction != Direction.UP) {
						direction = Direction.DOWN;
						break;
					}
				case A:
				case LEFT:
					if(direction!=Direction.RIGHT) {
						direction = Direction.LEFT;
						break;
					}
				case D:
				case RIGHT:
					if(direction!=Direction.LEFT) {
						direction = Direction.RIGHT;
						break;
					}
				case SPACE:
					if(timeline.getStatus()==Animation.Status.PAUSED) {
						timeline.play();
						scoreLabel.setText("Score: " + score);
					} else {
						scoreLabel.setText("PAUSE");
						timeline.pause();
					}
					break;
				case ESCAPE:
					timeline.stop();
					timeline.getKeyFrames().clear();
					window.setScene(new Scene(createStartScreen(), GAME_WIDTH, GAME_HEIGHT));
					break;
				case P:
					mediaPlayer.setMute(!isMute);
					isMute = !isMute;
					break;
				default: {}
				}
			}
		});
	}
	
	/**
	 * Start Szene mit Controller-Buttons
	 * @return
	 */
	private BorderPane createStartScreen() {
		BorderPane root = new BorderPane();
		
		// Start
		Label startLabel = new Label("Snake");
		Image image = new Image("/images/snake-icon.png");
		ImageView imageView = new ImageView(image);
		imageView.setFitWidth(50);
		imageView.setFitHeight(50);
		startLabel.setGraphic(imageView);
		startLabel.setFont(new Font("Airstream", 50));
		startLabel.setGraphicTextGap(30);
		
		Button startButton = new Button("Start");
		startButton.setPrefWidth(180);
		startButton.setFont(new Font("Arial", 24));
		startButton.setOnAction((ActionEvent event) -> {
			Scene scene = new Scene(createGameContent());
			window.setScene(scene);
			window.setResizable(false);
			window.setTitle("Snake Game");
			
			window.show();
			
			// Game Mechanismus starten
			keypressed(scene);
			startGame();
		});
		
		VBox startTitle = new VBox(5);
		startTitle.setAlignment(Pos.CENTER);
		startTitle.setMaxHeight(80);
		startTitle.getChildren().addAll(startLabel, startButton);
		root.setTop(startTitle);
	
		// Exit
		Button exitButton = new Button("Exit");
		ImageView exitImage = new ImageView(new Image("/images/door_in.png"));
		exitButton.setGraphic(exitImage);
		exitButton.setGraphicTextGap(50);
		exitButton.setFont(new Font("Arial", 24));
		exitButton.setPrefWidth(180);
		root.setBottom(exitButton);
		BorderPane.setAlignment(exitButton, Pos.CENTER);
		BorderPane.setMargin(exitButton, new Insets(20));
		exitButton.setOnAction((ActionEvent event) -> {
			Platform.exit();
		});
		
		// Einstellungen
		Button gameSpeed = new Button("Speed");
		gameSpeed.setFont(new Font("Arial", 24));
		gameSpeed.setPrefWidth(180);
		Button borderButton = new Button("Rand");
		borderButton.setFont(new Font("Arial", 24));
		borderButton.setPrefWidth(180);
		ImageView borderImage = new ImageView(new Image("/images/accept_button.png"));
		borderButton.setGraphic(borderImage);
		borderButton.setGraphicTextGap(50);
		Label speedLabel = new Label("Leicht");
		speedLabel.setFont(new Font("Arial", 24));
		speedLabel.setPrefWidth(180);
		speedLabel.setAlignment(Pos.CENTER);
		
		VBox controllButtons = new VBox(5);
		controllButtons.setMaxWidth(300);
		controllButtons.setAlignment(Pos.CENTER);
		controllButtons.setBackground(new Background(new BackgroundFill(Color.ANTIQUEWHITE, 
				new CornerRadii(4), new Insets(10))));
		Separator ctrlSeparator = new Separator();
		ctrlSeparator.setOrientation(Orientation.HORIZONTAL);
		ctrlSeparator.setMaxWidth(180);
		ctrlSeparator.setPrefHeight(24);
		controllButtons.getChildren().addAll(gameSpeed, speedLabel, ctrlSeparator,
				borderButton);
		root.setCenter(controllButtons);
		BorderPane.setAlignment(controllButtons, Pos.CENTER_RIGHT);
		
		gameSpeed.setOnAction((ActionEvent event) -> {
			switch (speed) {
			case SLOW:
				speed = Speed.MIDDLE;
				speedLabel.setText("Mittel");
				break;
			case MIDDLE:
				speed = Speed.FAST;
				speedLabel.setText("Schwer");
				break;
			case FAST:
				speed = Speed.SLOW;
				speedLabel.setText("Leicht");
				break;
			default: {}
			}
		});
		
		borderButton.setOnAction((ActionEvent event) -> {
			if(isEndless) {
				isEndless = false;
				ImageView tempImage = new ImageView(new Image("/images/accept_button.png"));
				borderButton.setGraphic(tempImage);
			} else {
				isEndless = true;
				ImageView tempImage = new ImageView(new Image("/images/cross.png"));
				borderButton.setGraphic(tempImage);
			}
		});
		
		// Musik und Töne
		Button soundButton = new Button("", new ImageView(new Image("/images/sound.png")));
		soundButton.setPrefWidth(180);
		soundButton.setAlignment(Pos.CENTER);
		soundButton.setOnAction((ActionEvent event) -> {
			if(isMute) {
				soundButton.setGraphic(new ImageView(new Image("/images/sound.png")));
				mediaPlayer.play();
				isMute = false;
			} else {
				soundButton.setGraphic(new ImageView(new Image("/images/sound_mute.png")));
				mediaPlayer.pause();
				isMute = true;
			}
		});
		
		VBox musicBox = new VBox(5);
		musicBox.setAlignment(Pos.CENTER_RIGHT);
		musicBox.setPrefWidth(180);
		musicBox.setPadding(new Insets(5));
		musicBox.getChildren().add(soundButton);
		
		Separator musicSeparator = new Separator();
		musicBox.getChildren().add(musicSeparator);
		
		root.setRight(musicBox);
		
		volumeSlider.setMajorTickUnit(0.2);
		volumeSlider.setMin(0);
		volumeSlider.setMax(1);
		volumeSlider.setValue(0.2);
		volumeSlider.setShowTickMarks(true);
		volumeLabel.setMinWidth(24);
		volumeLabel.setAlignment(Pos.CENTER_RIGHT);
		
		volumeSlider.valueProperty().addListener(new InvalidationListener() {

			@Override
			public void invalidated(Observable obs) {
				volumeLabel.setText(""+ (int)(volumeSlider.getValue()*100));
				mediaPlayer.setVolume(volumeSlider.getValue());
			}
		});
		
		HBox volumeBox = new HBox(10);
		volumeBox.getChildren().addAll(volumeSlider, volumeLabel);
		musicBox.getChildren().add(volumeBox);
		
		return root;
	}
	
	private void playMusic(String title) {
		String musicFile = title;
		URL fileUrl = getClass().getResource(musicFile);

		Media media = new Media(fileUrl.toString());
		mediaPlayer = new MediaPlayer(media);
		mediaPlayer.play();
		mediaPlayer.setVolume(0.2);
		mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
	}

}
