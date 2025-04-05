package RubiksCubeSolver3D;

import javafx.animation.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.text.*;
import javafx.scene.text.Font;
import javafx.scene.transform.*;
import javafx.stage.Stage;
import javafx.util.*;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rubik's Cube Solver 3D 
 * v1.0
 * @author Spina Luca
 */

public class RubiksCubeSolver3D extends Application {

    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int SCREEN_WIDTH = 1126;
    private static final int SCREEN_HEIGHT = 700;

    private static final int CUBE_PANEL_WIDTH = 600;
    private static final int CUBE_PANEL_HEIGHT = SCREEN_HEIGHT;
    private static final int CUBE_DIMENSION = 3;
    private static final int FACES = 6;
    private static final int CUBE_SIDE = 100;
    private static final int[][] centralPieces = {
            {1, 1, 2},
            {1, 2, 1},
            {1, 1, 0},
            {1, 0, 1},
            {0, 1, 1},
            {2, 1, 1}
    };
    private static final int[][] edgePieces = {
            {1, 0, 2},
            {1, 2, 2},
            {1, 2, 0},
            {1, 0, 0},
            {0, 1, 2},
            {2, 1, 2},
            {2, 1, 0},
            {0, 1, 0},
            {0, 0, 1},
            {0, 2, 1},
            {2, 2, 1},
            {2, 0, 1}
    };
    private static final int[][] cornerPieces = {
            {0, 0, 2},
            {0, 2, 2},
            {2, 2, 2},
            {2, 0, 2},
            {0, 0, 0},
            {0, 2, 0},
            {2, 2, 0},
            {2, 0, 0}
    };
    private final int[][] keepFaces = {
            {4, 5},
            {1, 3},
            {0, 2},
    };
    private final int[][] switchFaces = {
            {0, 1, 2, 3, 0, 1, 2, 3},
            {0, 4, 2, 5, 0, 4, 2, 5},
            {1, 5, 3, 4, 1, 5, 3, 4},
    };

    private static final int CONTROL_PANEL_WIDTH = SCREEN_WIDTH - CUBE_PANEL_WIDTH;
    private static final int CONTROL_PANEL_HEIGHT = SCREEN_HEIGHT;
    private static final int PANEL_X = CUBE_PANEL_WIDTH;
    private static final int PANEL_Y = 0;
    private static final int TEXTS_X = 30;
    private static final int BUTTONS_Y = 50;
    private static final int BUTTONS_X = TEXTS_X + 190;
    private static final int TEXTS_Y = BUTTONS_Y + 43;
    private static final int LAYER1_BUTTON_WIDTH = 80;
    private static final int LAYER1_BUTTON_HEIGHT = 68;
    private static final int COLOR_BUTTONS_SIZE = 26;
    private static final int MOVE_BUTTON_SIZE = 36;

    private ArrayList<Button> buttons = new ArrayList<>();
    private Slider speedSlider;
    private Text speedSliderUnitText;
    private Label warningPopup = new Label();
    private Label playedMovesPopup = new Label();
    private ScrollPane playedMovesScrollPane = new ScrollPane();
    private Text playedMovesText = new Text();

    private final Font fredokaOne = Font.loadFont(getClass().getResourceAsStream("/resources/fonts/FredokaOne-Regular.ttf"), 35);
    private final Font signikaRegular = Font.loadFont(getClass().getResourceAsStream("/resources/fonts/Signika-Regular.ttf"), 16);
    private final AudioClip moveSound = new AudioClip(getClass().getResource("/resources/sounds/moveSound.mp3").toExternalForm());
    private final ImageView activatedAudioButtonImage = new ImageView(new Image("/resources/images/activatedAudio.png"));
    private final ImageView disactivatedAudioButtonImage = new ImageView(new Image("/resources/images/disactivatedAudio.png"));

    private static double rotationDuration = 0.1;
    private static double rotationDelay = 0.1;
    private static final int SCRAMBLE_SIZE = 20;
    private static boolean resetFlag = false;

    private final PhongMaterial[] colors = setupColors();
    private PhongMaterial selectedColor;
    private Box[][][][] cube = setupCube();
    private Box[][][][] fakeCube = cube;
    private final String[] PREDEFINED_MOVES = new String[]{"L", "L'", "R", "R'", "U", "U'", "D", "D'", "F", "F'", "B", "B'", "M", "M'", "E", "E'", "S", "S'", "x", "x'", "y", "y'", "z", "z'"};
    private ArrayList<String> solverMoves = new ArrayList<>();
    private Timeline rotationsTimeline = new Timeline();
    private Group cubeGroup = new Group();
    private final Pane cubePanel = setupCubePanel();
    private final Pane controlPanel = setupControlPanel();
    private final Group group = new Group();
    private Scene scene = setupScene();
    private final PerspectiveCamera camera = new PerspectiveCamera();

    private Rotate xRotate = new Rotate(20, Rotate.X_AXIS);
    private Rotate yRotate = new Rotate(10, Rotate.Y_AXIS);
    private Rotate zRotate = new Rotate(1.5, Rotate.Z_AXIS);
    private double anchorX, anchorY;
    private double anchorAngleX = 0, anchorAngleY = 0;
    private final DoubleProperty angleX = new SimpleDoubleProperty(0);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);
    private final int ANGLE_BY_KEY = 10;

    private int countX, countY, countZ, countFace;
    private int stopCount;

    @Override
    public void start(Stage stage) {
        addCubeToCubeGroup();
        cubeGroup.getTransforms().addAll(xRotate, yRotate, zRotate);
        cubeGroup.setTranslateX((double) (CUBE_PANEL_WIDTH / 2) + 25);
        cubeGroup.setTranslateY((double) (CUBE_PANEL_HEIGHT / 2) - 15);

        cubeMouseControl();
        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            Rotate rotate = new Rotate();
            switch (event.getCode()) {
                case S:
                case NUMPAD2:
                    rotate = new Rotate(ANGLE_BY_KEY, Rotate.X_AXIS);
                    break;
                case W:
                case NUMPAD8:
                    rotate = new Rotate(- ANGLE_BY_KEY, Rotate.X_AXIS);
                    break;
                case A:
                case NUMPAD4:
                    rotate = new Rotate(ANGLE_BY_KEY, Rotate.Y_AXIS);
                    break;
                case D:
                case NUMPAD6:
                    rotate = new Rotate(- ANGLE_BY_KEY, Rotate.Y_AXIS);
                    break;
                case E:
                case NUMPAD9:
                    rotate = new Rotate(ANGLE_BY_KEY, Rotate.Z_AXIS);
                    break;
                case Q:
                case NUMPAD7:
                    rotate = new Rotate(- ANGLE_BY_KEY, Rotate.Z_AXIS);
                    break;
            }
            cubeGroup.getTransforms().add(rotate);
        });

        group.getChildren().addAll(cubeGroup, cubePanel, controlPanel);
        group.translateXProperty().bind(scene.widthProperty().subtract(SCREEN_WIDTH).divide(2));
        group.translateYProperty().bind(scene.heightProperty().subtract(SCREEN_HEIGHT).divide(2));
        group.scaleYProperty().bind(scene.heightProperty().divide(SCREEN_HEIGHT));
        group.scaleXProperty().bind(scene.heightProperty().multiply(SCREEN_WIDTH).divide(SCREEN_HEIGHT).divide(SCREEN_WIDTH));
        group.scaleZProperty().bind(group.scaleYProperty());

        scene.setFill(Color.rgb(80, 80, 80));
        scene.setCamera(camera);
        scene.getStylesheets().addAll("resources/css/buttonStyle.css", "resources/css/sliderStyle.css", "resources/css/scrollPaneStyle.css");

        stage.setTitle("Rubik's Cube Solver 3D");
        stage.getIcons().add(new Image("resources/images/icon.png"));
        stage.setScene(scene);
        stage.show();
        manageWindowSizes(stage);
    }

    private Image[] colorImages() {
        Image[] colorImages = new Image[7];
        colorImages[0] = new Image("resources/images/colors/white.jpg");
        colorImages[1] = new Image("resources/images/colors/red.jpg");
        colorImages[2] = new Image("resources/images/colors/yellow.jpg");
        colorImages[3] = new Image("resources/images/colors/orange.jpg");
        colorImages[4] = new Image("resources/images/colors/blue.jpg");
        colorImages[5] = new Image("resources/images/colors/green.jpg");
        colorImages[6] = new Image("resources/images/colors/black.jpg");
        return colorImages;
    }

    private PhongMaterial[] setupColors() {
        PhongMaterial[] colors = new PhongMaterial[7];
        Image[] colorImages = colorImages();
        for (int count = 0; count < 7; count ++) {
            colors[count] = new PhongMaterial();
            colors[count].setDiffuseMap(colorImages[count]);
        }
        return colors;
    }

    private String getColorByNumber(int number) {
        String color = null;
        switch (number) {
            case 0:
                color = "white";
                break;
            case 1:
                color = "red";
                break;
            case 2:
                color = "yellow";
                break;
            case 3:
                color = "orange";
                break;
            case 4:
                color = "blue";
                break;
            case 5:
                color = "green";
                break;
            case 6:
                color = "black";
                break;
        }
        return color;
    }

    private Box[][][][] setupCube() {
        Box[][][][] cube = new Box[CUBE_DIMENSION][CUBE_DIMENSION][CUBE_DIMENSION][FACES];
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++) {
                        cube[countX][countY][countZ][countFace] = new Box(CUBE_SIDE, CUBE_SIDE, CUBE_SIDE);
                        cube[countX][countY][countZ][countFace].setTranslateX(- CUBE_SIDE + CUBE_SIDE * countX);
                        cube[countX][countY][countZ][countFace].setTranslateY(CUBE_SIDE - CUBE_SIDE * countY);
                        cube[countX][countY][countZ][countFace].setTranslateZ(CUBE_SIDE - CUBE_SIDE * countZ);
                        cube[countX][countY][countZ][countFace].setMaterial(colors[6]);
                    }

        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++) {
                    switch (countX) {
                        case 0:
                            cube[countX][countY][countZ][4].setMaterial(colors[4]);
                            cube[countX][countY][countZ][4].setTranslateX(cube[countX][countY][countZ][4].getTranslateX() - 1);
                            cube[countX][countY][countZ][5].setTranslateX(cube[countX][countY][countZ][5].getTranslateX() + 0.1);
                            break;
                        case 2:
                            cube[countX][countY][countZ][5].setMaterial(colors[5]);
                            cube[countX][countY][countZ][5].setTranslateX(cube[countX][countY][countZ][5].getTranslateX() + 1);
                            cube[countX][countY][countZ][4].setTranslateX(cube[countX][countY][countZ][4].getTranslateX() - 0.1);
                        default:
                            cube[countX][countY][countZ][4].setTranslateX(cube[countX][countY][countZ][4].getTranslateX() - 0.1);
                            cube[countX][countY][countZ][5].setTranslateX(cube[countX][countY][countZ][5].getTranslateX() + 0.1);
                    }

                    switch (countY) {
                        case 0:
                            cube[countX][countY][countZ][3].setMaterial(colors[3]);
                            cube[countX][countY][countZ][3].setTranslateY(cube[countX][countY][countZ][3].getTranslateY() + 1);
                            cube[countX][countY][countZ][1].setTranslateY(cube[countX][countY][countZ][1].getTranslateY() - 0.1);
                            break;
                        case 2:
                            cube[countX][countY][countZ][1].setMaterial(colors[1]);
                            cube[countX][countY][countZ][1].setTranslateY(cube[countX][countY][countZ][1].getTranslateY() - 1);
                            cube[countX][countY][countZ][3].setTranslateY(cube[countX][countY][countZ][3].getTranslateY() + 0.1);
                        default:
                            cube[countX][countY][countZ][3].setTranslateY(cube[countX][countY][countZ][3].getTranslateY() + 0.1);
                            cube[countX][countY][countZ][1].setTranslateY(cube[countX][countY][countZ][1].getTranslateY() - 0.1);
                    }

                    switch (countZ) {
                        case 0:
                            cube[countX][countY][countZ][2].setMaterial(colors[2]);
                            cube[countX][countY][countZ][2].setTranslateZ(cube[countX][countY][countZ][2].getTranslateZ() + 1);
                            cube[countX][countY][countZ][0].setTranslateZ(cube[countX][countY][countZ][0].getTranslateZ() - 0.1);
                            break;
                        case 2:
                            cube[countX][countY][countZ][0].setMaterial(colors[0]);
                            cube[countX][countY][countZ][0].setTranslateZ(cube[countX][countY][countZ][0].getTranslateZ() - 1);
                            cube[countX][countY][countZ][2].setTranslateZ(cube[countX][countY][countZ][2].getTranslateZ() + 0.1);
                        default:
                            cube[countX][countY][countZ][0].setTranslateZ(cube[countX][countY][countZ][0].getTranslateZ() - 0.1);
                            cube[countX][countY][countZ][2].setTranslateZ(cube[countX][countY][countZ][2].getTranslateZ() + 0.1);
                    }
                }

        return cube;
    }

    private Pane setupCubePanel() {
        Pane panel = new Pane();
        panel.setPrefSize(CUBE_PANEL_WIDTH, CUBE_PANEL_HEIGHT);

        Text authorText = new Text("Rubik's Cube Solver 3D v1.0\nAuthor: Spina Luca");
        authorText.setFont(signikaRegular);
        authorText.setStyle("-fx-fill: grey;\n" +
                "-fx-text-alignment: center;");
        authorText.setTranslateX((CUBE_PANEL_WIDTH - authorText.getBoundsInLocal().getWidth()) / 2);
        authorText.setTranslateY(CUBE_PANEL_HEIGHT - authorText.getBoundsInLocal().getHeight());
        panel.getChildren().add(authorText);

        Button audioButton = new Button("", activatedAudioButtonImage);
        audioButton.setStyle("-fx-background-color: transparent;\n" +
                "-fx-border-color: transparent;");
        activatedAudioButtonImage.setFitHeight(50);
        activatedAudioButtonImage.setPreserveRatio(true);
        disactivatedAudioButtonImage.setFitHeight(50);
        disactivatedAudioButtonImage.setPreserveRatio(true);
        audioButton.setTranslateY(authorText.getTranslateY() - (activatedAudioButtonImage.getBoundsInLocal().getHeight() / 2));
        Tooltip audioButtonTooltip = new Tooltip("Mute audio.");
        audioButtonTooltip.setFont(new Font(signikaRegular.getSize()));
        audioButtonTooltip.setStyle("-fx-font-size: 10pt;");
        audioButton.setTooltip(audioButtonTooltip);
        AtomicInteger countAudio = new AtomicInteger();
        audioButton.setOnAction(event -> {
            moveSound.setVolume(countAudio.intValue() % 2);
            if ((countAudio.intValue() % 2) == 0) {
                audioButton.setGraphic(disactivatedAudioButtonImage);
                audioButtonTooltip.setText("Active audio.");
            }
            else {
                audioButton.setGraphic(activatedAudioButtonImage);
                audioButtonTooltip.setText("Mute audio.");
            }
            countAudio.getAndIncrement();
        });
        panel.getChildren().add(audioButton);

        return panel;
    }

    private Pane setupControlPanel() {
        Pane panel = new Pane();
        panel.setTranslateX(PANEL_X);
        panel.setTranslateY(PANEL_Y);
        panel.setPrefSize(CONTROL_PANEL_WIDTH, CONTROL_PANEL_HEIGHT);
        panel.setStyle("-fx-background-image: url(\"resources/images/controlPanelFrame.png\");\n" +
                "-fx-background-width: "+ (CONTROL_PANEL_WIDTH - 20) +"px;\n" +
                "-fx-background-position: " + (TEXTS_X - 30) + " " + (BUTTONS_Y - 30) + ";\n" +
                "-fx-background-repeat: no-repeat;");

        ArrayList<Text> texts = new ArrayList<>();
        ArrayList<Tooltip> tooltips = new ArrayList<>();

        Text functionsText = new Text("Functions:");
        functionsText.setTranslateX(TEXTS_X);
        functionsText.setTranslateY(TEXTS_Y);
        texts.add(functionsText);

        Text colorsText = new Text("Colors:");
        colorsText.setTranslateX(TEXTS_X);
        colorsText.setTranslateY(functionsText.getTranslateY() + 95);
        texts.add(colorsText);

        Text movesText = new Text("Moves:");
        movesText.setTranslateX(TEXTS_X);
        movesText.setTranslateY(colorsText.getTranslateY() + 144);
        texts.add(movesText);

        Text speedText = new Text("Speed:");
        speedText.setTranslateX(TEXTS_X);
        speedText.setTranslateY(movesText.getTranslateY() + 126);
        texts.add(speedText);

        for (Text text : texts) {
            setTextPredefinedStyle(text);
            panel.getChildren().add(text);
        }

        Button resetButton = new Button("Reset");
        buttons.add(resetButton);
        tooltips.add(new Tooltip("Reset Rubik's cube to a solved position."));
        resetButton.setPrefSize(LAYER1_BUTTON_WIDTH, LAYER1_BUTTON_HEIGHT);
        resetButton.setTranslateX(BUTTONS_X);
        resetButton.setTranslateY(BUTTONS_Y);
        resetButton.setOnAction(event -> reset());

        Button scrambleButton = new Button("Scramble");
        buttons.add(scrambleButton);
        tooltips.add(new Tooltip("Scramble Rubik's cube using " + SCRAMBLE_SIZE + " randomized moves."));
        scrambleButton.setPrefSize(LAYER1_BUTTON_WIDTH, LAYER1_BUTTON_HEIGHT);
        scrambleButton.setTranslateX(resetButton.getTranslateX() + LAYER1_BUTTON_WIDTH + 8);
        scrambleButton.setTranslateY(BUTTONS_Y);
        scrambleButton.setOnAction(event -> scramble());

        Button solverButton = new Button("Fridrich\n(CFOP)\nSolver");
        buttons.add(solverButton);
        tooltips.add(new Tooltip("Solve Rubik's cube using the Fridrich (CFOP) method."));
        solverButton.setPrefSize(LAYER1_BUTTON_WIDTH, LAYER1_BUTTON_HEIGHT);
        solverButton.setTranslateX(scrambleButton.getTranslateX() + LAYER1_BUTTON_WIDTH + 8);
        solverButton.setTranslateY(BUTTONS_Y);
        solverButton.setOnAction(event -> solver());

        Button[] colorButtons = new Button[FACES];
        Image[] colorImages = colorImages();
        for (int count = 0; count < FACES; count ++) {
            ImageView colorImageView = new ImageView(colorImages[count]);
            colorImageView.setFitWidth(COLOR_BUTTONS_SIZE);
            colorImageView.setFitHeight(COLOR_BUTTONS_SIZE);
            colorButtons[count] = new Button("", colorImageView);
            int finalCount = count;
            colorButtons[count].setOnAction(event -> {
                for (int count2 = 0; count2 < FACES; count2 ++)
                    colorButtons[count2].setDisable(false);
                colorButtons[finalCount].setDisable(true);
                selectedColor = colors[finalCount];
                updateColorCube();
            });
            buttons.add(colorButtons[count]);
            tooltips.add(new Tooltip("Select the color " + getColorByNumber(count) + " and paste it on a cube by clicking on it."));
        }
        colorButtons[0].setTranslateX(BUTTONS_X + 16);
        colorButtons[0].setTranslateY(BUTTONS_Y + LAYER1_BUTTON_HEIGHT + 18);
        for (int count = 1; count < FACES; count ++) {
            colorButtons[count].setTranslateX(colorButtons[count - 1].getTranslateX() + COLOR_BUTTONS_SIZE + 62);
            colorButtons[count].setTranslateY(colorButtons[count - 1].getTranslateY());
            if (count == FACES / 2) {
                colorButtons[count].setTranslateX(colorButtons[0].getTranslateX());
                colorButtons[count].setTranslateY(colorButtons[0].getTranslateY() + COLOR_BUTTONS_SIZE + 20);
            }
        }

        Button[] moveButtons = new Button[PREDEFINED_MOVES.length];
        for (int count = 0; count < PREDEFINED_MOVES.length; count ++) {
            moveButtons[count] = new Button(PREDEFINED_MOVES[count]);
            moveButtons[count].setPrefSize(MOVE_BUTTON_SIZE, MOVE_BUTTON_SIZE);
            int finalCount = count;
            moveButtons[count].setOnAction(event -> {
                playMoves(new String[]{PREDEFINED_MOVES[finalCount]});

                if (playedMovesText.getText().equals(""))
                    playedMovesText.setText("User move (1):\n");
                else {
                    for (int count2 = playedMovesText.getText().length() - 1; count2 > 0; count2 --)
                        if (playedMovesText.getText().charAt(count2) == '(') {
                            if (playedMovesText.getText().startsWith("User move", count2 - 10) || playedMovesText.getText().startsWith("User moves", count2 - 11)) {
                                int count3 = count2 + 1, totalMoves;
                                String numberString = new String();
                                while (playedMovesText.getText().charAt(count3) != ')') {
                                    numberString += playedMovesText.getText().charAt(count3);
                                    count3 ++;
                                }
                                totalMoves = Integer.parseInt(numberString) + 1;
                                if (totalMoves == 2)
                                    playedMovesText.setText(playedMovesText.getText().substring(0, 9) + 's' + playedMovesText.getText().substring(9, count2 + 1) + totalMoves + playedMovesText.getText().substring(count3));
                                else if (totalMoves > 2)
                                    playedMovesText.setText(playedMovesText.getText().substring(0, count2 + 1) + totalMoves + playedMovesText.getText().substring(count3));
                                playedMovesText.setText(playedMovesText.getText() + ", ");
                            }
                            else
                                playedMovesText.setText("User move (1):\n");
                            break;
                        }
                }
                playedMovesText.setText(playedMovesText.getText() + PREDEFINED_MOVES[finalCount]);
                playedMovesPopup.setVisible(true);
                playedMovesScrollPane.setVisible(true);
            });
            buttons.add(moveButtons[count]);
            tooltips.add(new Tooltip("Make move " + PREDEFINED_MOVES[count] + "."));
        }
        for (int count = 0; count < (PREDEFINED_MOVES.length / 2) - 1; count += 2) {
            if (count > 0) {
                moveButtons[count].setTranslateX(moveButtons[count - 2].getTranslateX() + MOVE_BUTTON_SIZE + 8);
                moveButtons[count + 1].setTranslateX(moveButtons[count - 2].getTranslateX() + MOVE_BUTTON_SIZE + 8);
                moveButtons[count + (PREDEFINED_MOVES.length / 2)].setTranslateX(moveButtons[count - 2].getTranslateX() + MOVE_BUTTON_SIZE + 8);
                moveButtons[count + (PREDEFINED_MOVES.length / 2) + 1].setTranslateX(moveButtons[count - 2].getTranslateX() + MOVE_BUTTON_SIZE + 8);
            }
            else {
                moveButtons[count].setTranslateX(BUTTONS_X);
                moveButtons[count + 1].setTranslateX(moveButtons[count].getTranslateX());
                moveButtons[count + (PREDEFINED_MOVES.length / 2)].setTranslateX(moveButtons[count].getTranslateX());
                moveButtons[count + (PREDEFINED_MOVES.length / 2) + 1].setTranslateX(moveButtons[count].getTranslateX());
            }
            moveButtons[count].setTranslateY(colorButtons[FACES / 2].getTranslateY() + COLOR_BUTTONS_SIZE + 34);
            moveButtons[count + 1].setTranslateY(moveButtons[count].getTranslateY() + MOVE_BUTTON_SIZE + 6);
            moveButtons[count + (PREDEFINED_MOVES.length / 2)].setTranslateY(moveButtons[count + 1].getTranslateY() + MOVE_BUTTON_SIZE + 6);
            moveButtons[count + (PREDEFINED_MOVES.length / 2) + 1].setTranslateY(moveButtons[count + (PREDEFINED_MOVES.length / 2)].getTranslateY() + MOVE_BUTTON_SIZE + 6);
        }

        speedSlider = new Slider(0.1, 2.1, 0.1);
        speedSlider.setTranslateX(BUTTONS_X);
        speedSlider.setTranslateY(speedText.getTranslateY() - 16);
        speedSlider.focusedProperty().addListener((ov, t, t1) -> Platform.runLater(speedSlider::requestFocus));
        Tooltip speedSliderTooltip = new Tooltip("Manage the speed of Rubik's cube movements.");
        speedSliderTooltip.setFont(new Font(signikaRegular.getSize()));
        speedSliderTooltip.setStyle("-fx-font-size: 10pt;");
        speedSlider.setTooltip(speedSliderTooltip);
        speedSlider.setPrefWidth(256);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(0.2);
        speedSlider.setMinorTickCount(1);
        speedSlider.setBlockIncrement(speedSlider.getMajorTickUnit() / speedSlider.getMax());
        speedSlider.setSnapToTicks(true);
        speedSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                return ((Float) value.floatValue()).toString().substring(0, 3) + 's';
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        speedSlider.valueProperty().addListener(event -> rotationDuration = speedSlider.getValue());
        panel.getChildren().add(speedSlider);

        speedSliderUnitText = new Text("Time per move");
        speedSliderUnitText.setTranslateX(speedSlider.getTranslateX() + ((speedSlider.getPrefWidth() - speedSliderUnitText.getBoundsInLocal().getWidth()) / 2));
        speedSliderUnitText.setTranslateY(speedSlider.getTranslateY() - 10);
        speedSliderUnitText.setStyle("-fx-font-size: 12px;\n" +
                "-fx-fill: white;");
        panel.getChildren().add(speedSliderUnitText);

        for (int count = 0; count < buttons.size(); count ++) {
            setButtonPredefinedStyle(buttons.get(count), tooltips.get(count));
            panel.getChildren().add(buttons.get(count));
        }

        playedMovesPopup.setPrefSize(288, 160);
        playedMovesPopup.setTranslateX((CONTROL_PANEL_WIDTH - playedMovesPopup.getPrefWidth()) / 2);
        playedMovesPopup.setTranslateY(speedText.getTranslateY() + 35);
        playedMovesPopup.setStyle("-fx-background-image: url(\"resources/images/playedMovesPopupBackground.png\");\n" +
                "-fx-background-size: 288 160;");
        playedMovesPopup.setVisible(false);
        panel.getChildren().add(playedMovesPopup);

        playedMovesText.setStyle("-fx-fill: #0012d3;\n" +
                "-fx-font-size: 12pt;\n" +
                "-fx-font-weight: bold;\n" +
                "-fx-line-spacing: -5;");
        playedMovesText.setTranslateY(-5);
        playedMovesText.setWrappingWidth(playedMovesPopup.getPrefWidth() - 22);

        playedMovesScrollPane.setPrefSize(282, 65);
        playedMovesScrollPane.setTranslateX(playedMovesPopup.getTranslateX() + 2);
        playedMovesScrollPane.setTranslateY(playedMovesPopup.getTranslateY() + 85);
        playedMovesScrollPane.setStyle("-fx-padding: 0 5;");
        playedMovesScrollPane.setContent(playedMovesText);
        playedMovesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        playedMovesScrollPane.setVisible(false);
        panel.getChildren().add(playedMovesScrollPane);

        warningPopup.setPrefSize(288, 160);
        warningPopup.setTranslateX((CONTROL_PANEL_WIDTH - warningPopup.getPrefWidth()) / 2);
        warningPopup.setTranslateY(speedText.getTranslateY() + 35);
        warningPopup.setWrapText(true);
        warningPopup.setStyle("-fx-background-image: url(\"resources/images/warningPopupBackground.png\");\n" +
                "-fx-background-size: 288 160;\n" +
                "-fx-text-fill: #980a10;\n" +
                "-fx-padding: 0 5 -80 5;\n" +
                "-fx-font-size: 12pt;\n" +
                "-fx-font-weight: bold;\n" +
                "-fx-alignment: center;\n" +
                "-fx-text-alignment: center;\n" +
                "-fx-line-spacing: -7;");
        warningPopup.setVisible(false);
        panel.getChildren().add(warningPopup);

        return panel;
    }

    private Scene setupScene() {
        if (screenSize.getWidth() > screenSize.getHeight())
            return new Scene(group, screenSize.getHeight() / 1080 * SCREEN_WIDTH, SCREEN_HEIGHT * screenSize.getHeight() / 1080, true);
        else
            return new Scene(group, SCREEN_WIDTH * screenSize.getWidth() / 1920, screenSize.getWidth() / 1920 * SCREEN_HEIGHT, true);
    }

    private void manageWindowSizes(Stage stage) {
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
        stage.minWidthProperty().bind(stage.heightProperty().multiply(stage.getWidth() / stage.getHeight()));
    }

    private void setDisableControls(boolean value) {
        for (Button button : buttons)
            if (!button.getText().equals("Reset"))
                button.setDisable(value);
        speedSlider.setDisable(value);
        if (value)
            speedSliderUnitText.setOpacity(0.4);
        else
            speedSliderUnitText.setOpacity(1);
    }

    private void setButtonPredefinedStyle (Button button, Tooltip tooltip) {
        button.focusedProperty().addListener((ov, t, t1) -> Platform.runLater(button::requestFocus));
        if (!button.getText().isEmpty()) {
            button.setPadding(Insets.EMPTY);
            button.setFont(signikaRegular);
            button.setTextAlignment(TextAlignment.CENTER);
        }
        tooltip.setStyle("-fx-font-size: 10pt;");
        button.setTooltip(tooltip);
    }

    private void setTextPredefinedStyle (Text text) {
        text.setFont(fredokaOne);
    }

    private void layerRotation(String move) {
        Group moveGroup = new Group();
        RotateTransition rotation = new RotateTransition(Duration.seconds(rotationDuration), moveGroup);
        int refCount = -1, refValue = -1;

        switch (move.charAt(0)) {
            case 'v':
                rotation.setAxis(Rotate.X_AXIS);
                refCount = 0;
                break;
            case 'h':
                rotation.setAxis(Rotate.Y_AXIS);
                refCount = 1;
                break;
            case 'd':
                rotation.setAxis(Rotate.Z_AXIS);
                refCount = 2;
                break;
        }

        switch (move.charAt(1)) {
            case 'l':
            case 'b':
                refValue = 0;
                break;
            case 'c':
                refValue = 1;
                break;
            case 't':
            case 'r':
            case 'f':
                refValue = 2;
                break;
        }

        switch (move.charAt(2)) {
            case 'r':
            case 't':
            case 'a':
                rotation.setByAngle(-90);
                break;
            case 'l':
            case 'b':
            case 'c':
                rotation.setByAngle(90);
                break;
        }

        moveGroup = addToMoveGroup(moveGroup, refCount, refValue);
        cubeGroup.getChildren().add(moveGroup);

        rotation.play();
        rotation.setOnFinished(finish -> {
            if (!resetFlag) {
                cube = recreateCube(cube, move);
                cubeGroup.getChildren().clear();
                addCubeToCubeGroup();
            }
        });
    }

    private Group addToMoveGroup(Group moveGroup, int refCount, int refValue) {
        int[] countXYZ = new int[CUBE_DIMENSION];
        for (countXYZ[0] = 0; countXYZ[0] < CUBE_DIMENSION; countXYZ[0] ++)
            for (countXYZ[1] = 0; countXYZ[1] < CUBE_DIMENSION; countXYZ[1] ++)
                for (countXYZ[2] = 0; countXYZ[2] < CUBE_DIMENSION; countXYZ[2] ++)
                    for (countFace = 0; countFace < FACES; countFace ++)
                        if(countXYZ[refCount] == refValue) moveGroup.getChildren().add(cube[countXYZ[0]][countXYZ[1]][countXYZ[2]][countFace]);
        return moveGroup;
    }

    private Box[][][][] recreateCube(Box[][][][] cube, String move) {
        Box[][][][] newCube = new Box[CUBE_DIMENSION][CUBE_DIMENSION][CUBE_DIMENSION][FACES];
        newCube = cloneCube(cube, newCube);

        int count, count2, count3, count4;
        int[] switchCoords = {0, 1, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0};
        int[] keepCoords = {0, 1, 2, 2, 2, 1, 0, 0, 0, 1, 2, 2, 2, 1, 0, 0};

        int refValue = -1;
        switch (move.charAt(1)) {
            case 'l':
            case 'b':
                refValue = 0;
                break;
            case 'c':
                refValue = 1;
                break;
            case 't':
            case 'r':
            case 'f':
                refValue = 2;
                break;
        }

        switch (move.charAt(2)) {
            case 'l':
                for (count = 0, count3 = 4; count3 < 8; count3 ++) {
                    count4 = count3 - 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[switchCoords[6 + count]][refValue][switchCoords[3 + count]][switchFaces[1][count3]].setMaterial(cube[switchCoords[3 + count]][refValue][switchCoords[count]][switchFaces[1][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[keepCoords[count]][refValue][keepCoords[6 + count]][keepFaces[1][count2]].setMaterial(cube[keepCoords[6 + count]][refValue][keepCoords[4 + count]][keepFaces[1][count2]].getMaterial());
                }
                break;
            case 'r':
                for (count = 0, count3 = 4; count3 > 0; count3 --) {
                    count4 = count3 + 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[switchCoords[count]][refValue][switchCoords[3 + count]][switchFaces[1][count3]].setMaterial(cube[switchCoords[9 + count]][refValue][switchCoords[count]][switchFaces[1][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[keepCoords[count]][refValue][keepCoords[6 + count]][keepFaces[1][count2]].setMaterial(cube[keepCoords[2 + count]][refValue][keepCoords[count]][keepFaces[1][count2]].getMaterial());
                }
                break;
            case 't':
                for (count = 0, count3 = 4; count3 < 8; count3 ++) {
                    count4 = count3 - 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[refValue][switchCoords[count]][switchCoords[3 + count]][switchFaces[0][count3]].setMaterial(cube[refValue][switchCoords[9 + count]][switchCoords[count]][switchFaces[0][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[refValue][keepCoords[count]][keepCoords[6 + count]][keepFaces[0][count2]].setMaterial(cube[refValue][keepCoords[2 + count]][keepCoords[count]][keepFaces[0][count2]].getMaterial());
                }
                break;
            case 'b':
                for (count = 0, count3 = 4; count3 > 0; count3 --) {
                    count4 = count3 + 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[refValue][switchCoords[6 + count]][switchCoords[3 + count]][switchFaces[0][count3]].setMaterial(cube[refValue][switchCoords[3 + count]][switchCoords[count]][switchFaces[0][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[refValue][keepCoords[count]][keepCoords[6 + count]][keepFaces[0][count2]].setMaterial(cube[refValue][keepCoords[6 + count]][keepCoords[4 + count]][keepFaces[0][count2]].getMaterial());
                }
                break;
            case 'c':
                for (count = 0, count3 = 4; count3 < 8; count3 ++) {
                    count4 = count3 - 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[switchCoords[count]][switchCoords[3 + count]][refValue][switchFaces[2][count3]].setMaterial(cube[switchCoords[9 + count]][switchCoords[count]][refValue][switchFaces[2][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[keepCoords[6 + count]][keepCoords[count]][refValue][keepFaces[2][count2]].setMaterial(cube[keepCoords[4 + count]][keepCoords[6 + count]][refValue][keepFaces[2][count2]].getMaterial());
                }
                break;
            case 'a':
                for (count = 0, count3 = 4; count3 > 0; count3 --) {
                    count4 = count3 + 1;
                    for (count2 = 0; count2 < CUBE_DIMENSION; count2 ++, count ++)
                        newCube[switchCoords[6 + count]][switchCoords[3 + count]][refValue][switchFaces[2][count3]].setMaterial(cube[switchCoords[3 + count]][switchCoords[count]][refValue][switchFaces[2][count4]].getMaterial());
                }
                for (count = 0; count < 8; count ++) {
                    for (count2 = 0; count2 < 2; count2 ++)
                        newCube[keepCoords[6 + count]][keepCoords[count]][refValue][keepFaces[2][count2]].setMaterial(cube[keepCoords[count]][keepCoords[2 + count]][refValue][keepFaces[2][count2]].getMaterial());
                }
                break;
        }
        return newCube;
    }

    private void updateColorCube() {
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++) {
                        final int finalCountX = countX;
                        final int finalCountY = countY;
                        final int finalCountZ = countZ;
                        final int finalCountFace = countFace;
                        cube[countX][countY][countZ][countFace].setOnMouseClicked(event -> {
                            cube[finalCountX][finalCountY][finalCountZ][finalCountFace].setMaterial(selectedColor);
                            fakeCube[finalCountX][finalCountY][finalCountZ][finalCountFace].setMaterial(selectedColor);
                            fieldsQuantity();
                            if (!warningPopup.isVisible())
                                possibleCube();
                        });
                    }
    }

    private void playMoves(String[] moves) {
        rotationsTimeline.getKeyFrames().clear();
        KeyFrame[] movements = new KeyFrame[moves.length];
        for (int count = 0; count < moves.length; count ++) {
            int finalCount = count;
            movements[finalCount] = new KeyFrame(Duration.seconds((rotationDuration + rotationDelay) * count), event -> {
                for (int count2 = 0; count2 < 3; count2 ++) {
                    String translatedMove = translateMove(moves[finalCount])[count2];
                    if (translatedMove != null) {
                        layerRotation(translatedMove);
                        moveSound.play();
                    }
                }
            });
            rotationsTimeline.getKeyFrames().add(movements[finalCount]);
        }
        rotationsTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds((rotationDuration + rotationDelay) * moves.length), event -> fakeCube = cloneCube(cube, fakeCube)));
        Platform.runLater(rotationsTimeline::play);
        setDisableControls(true);
        rotationsTimeline.setOnFinished(event -> setDisableControls(false));
    }

    private String[] translateMove(String singmasterMove) {
        String[] myMove = new String[3];
        switch (singmasterMove) {
            case "U":
                myMove[0] = "htl";
                break;
            case "U'":
                myMove[0] = "htr";
                break;
            case "E":
                myMove[0] = "hcr";
                break;
            case "E'":
                myMove[0] = "hcl";
                break;
            case "D":
                myMove[0] = "hbr";
                break;
            case "D'":
                myMove[0] = "hbl";
                break;
            case "L":
                myMove[0] = "vlb";
                break;
            case "L'":
                myMove[0] = "vlt";
                break;
            case "M":
                myMove[0] = "vcb";
                break;
            case "M'":
                myMove[0] = "vct";
                break;
            case "R":
                myMove[0] = "vrt";
                break;
            case "R'":
                myMove[0] = "vrb";
                break;
            case "F":
                myMove[0] = "dfc";
                break;
            case "F'":
                myMove[0] = "dfa";
                break;
            case "S":
                myMove[0] = "dcc";
                break;
            case "S'":
                myMove[0] = "dca";
                break;
            case "B":
                myMove[0] = "dba";
                break;
            case "B'":
                myMove[0] = "dbc";
                break;
            case "x":
                myMove[0] = "vlt";
                myMove[1] = "vct";
                myMove[2] = "vrt";
                break;
            case "x'":
                myMove[0] = "vlb";
                myMove[1] = "vcb";
                myMove[2] = "vrb";
                break;
            case "y":
                myMove[0] = "htl";
                myMove[1] = "hcl";
                myMove[2] = "hbl";
                break;
            case "y'":
                myMove[0] = "htr";
                myMove[1] = "hcr";
                myMove[2] = "hbr";
                break;
            case "z":
                myMove[0] = "dfc";
                myMove[1] = "dcc";
                myMove[2] = "dbc";
                break;
            case "z'":
                myMove[0] = "dfa";
                myMove[1] = "dca";
                myMove[2] = "dba";
                break;
        }
        return myMove;
    }

    private void fieldsQuantity() {
        String warningString = new String();
        int[] fieldsQuantity = new int[colors.length - 1];
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++) {
                        PhongMaterial material = (PhongMaterial) cube[countX][countY][countZ][countFace].getMaterial();
                        for (int count = 0; count < fieldsQuantity.length; count ++)
                            if (material == colors[count])
                                fieldsQuantity[count] ++;
                    }
        for (int count = 0; count < fieldsQuantity.length; count ++)
            if (fieldsQuantity[count] != CUBE_DIMENSION * CUBE_DIMENSION) {
                warningString += fieldsQuantity[count] + " " + getColorByNumber(count) + ", ";
            }
        for (int count = warningString.length() - 3; count >= 0; count --)
            if (warningString.charAt(count) == ',') {
                warningString = warningString.substring(0, count) + " and " + warningString.substring(count + 2, warningString.length() - 2);
                break;
            }
        if (warningString.length() != 0) {
            warningPopup.setText("Invalid scramble! There are " + warningString + " fields. You should have 9 of each.");
            warningPopup.setVisible(true);
            for (Button button : buttons)
                if (!button.getText().equals("Reset") && !button.getText().isEmpty())
                    button.setDisable(true);
        }
        else {
            warningPopup.setVisible(false);
            setDisableControls(false);
        }
    }

    private void possibleCube() {
        try {
            fakeCube = cube;
            whiteCross();
            intuitiveF2L();
            twoLookOLL();
            twoLookPLL();
            addToSolverMoves("x");
        } catch (Exception e) {}
        if (!compareTwoCubes(fakeCube, setupCube())) {
            warningPopup.setText("Invalid scramble! It is unsolvable.");
            warningPopup.setVisible(true);
            for (Button button : buttons)
                if (!button.getText().equals("Reset") && !button.getText().isEmpty())
                    button.setDisable(true);
        }
        else {
            warningPopup.setVisible(false);
            setDisableControls(false);
        }
        fakeCube = cube;
    }

    private void reset() {
        KeyFrame resetKeyFrame = new KeyFrame(Duration.seconds(0), event -> {
            rotationsTimeline.stop();
            resetFlag = true;
            cube = setupCube();
            cubeGroup.getChildren().clear();
            addCubeToCubeGroup();
            fakeCube = cloneCube(cube, fakeCube);
            if (angleX.get() != 0 || angleY.get() != 0) {
                angleX.set(0);
                angleY.set(0);
            }
            cubeGroup.getTransforms().clear();
            cubeGroup.getTransforms().addAll(xRotate = new Rotate(20, Rotate.X_AXIS),
                    yRotate = new Rotate(10, Rotate.Y_AXIS),
                    zRotate = new Rotate(1.5, Rotate.Z_AXIS));
            cubeMouseControl();
            warningPopup.setVisible(false);
            playedMovesPopup.setVisible(false);
            playedMovesScrollPane.setVisible(false);
            playedMovesText.setText("");
            setDisableControls(false);
        });
        KeyFrame resetKeyFrame2 = new KeyFrame(Duration.seconds(rotationDuration + rotationDelay), event -> resetFlag = false);
        Timeline resetTimeline = new Timeline(resetKeyFrame, resetKeyFrame2);
        Platform.runLater(resetTimeline::play);
    }

    private void scramble() {
        String[] scrambleMoves = new String[SCRAMBLE_SIZE];
        int randCount;
        Random rand = new Random();
        randCount = rand.nextInt(PREDEFINED_MOVES.length);
        scrambleMoves[0] = PREDEFINED_MOVES[randCount];
        for (int count = 1; count < SCRAMBLE_SIZE; count ++) {
            rand = new Random();
            do {
                randCount = rand.nextInt(PREDEFINED_MOVES.length);
            } while (PREDEFINED_MOVES[randCount].charAt(0) == scrambleMoves[count - 1].charAt(0));
            scrambleMoves[count] = PREDEFINED_MOVES[randCount];
        }
        playMoves(scrambleMoves);

        if (playedMovesText.getText().equals(""))
            playedMovesText.setText("Scramble moves (" + scrambleMoves.length + "):\n");
        else {
            for (int count = playedMovesText.getText().length() - 1; count > 0; count --)
                if (playedMovesText.getText().charAt(count) == '(') {
                    if (playedMovesText.getText().startsWith("Scramble moves", count - 15)) {
                        int count2 = count + 1, totalMoves;
                        String numberString = new String();
                        while (playedMovesText.getText().charAt(count2) != ')') {
                            numberString += playedMovesText.getText().charAt(count2);
                            count2 ++;
                        }
                        totalMoves = Integer.parseInt(numberString) + scrambleMoves.length;
                        playedMovesText.setText(playedMovesText.getText().substring(0, count + 1) + totalMoves + playedMovesText.getText().substring(count2));
                        playedMovesText.setText(playedMovesText.getText() + ", ");
                    }
                    else
                        playedMovesText.setText("Scramble moves (" + scrambleMoves.length + "):\n");
                    break;
                }
        }
        playedMovesText.setText(playedMovesText.getText() + Arrays.toString(scrambleMoves).substring(1, Arrays.toString(scrambleMoves).length() - 1));
        playedMovesPopup.setVisible(true);
        playedMovesScrollPane.setVisible(true);
    }

    private void solver() {
        solverMoves.removeAll(solverMoves);
        whiteCross();
        intuitiveF2L();
        twoLookOLL();
        twoLookPLL();
        addToSolverMoves("x");
        shortenSolverMoves();
        playMoves(solverMoves.toArray(new String[0]));
        playedMovesText.setText("Solver moves (" + solverMoves.size() + "):\n" + Arrays.toString(solverMoves.toArray()).substring(1, Arrays.toString(solverMoves.toArray()).length() - 1));
        playedMovesPopup.setVisible(true);
        playedMovesScrollPane.setVisible(true);
    }

    private void addToSolverMoves(String... moves) {
        for (String move : moves) {
            solverMoves.add(move);
            for (int count = 0; count < 3; count ++) {
                String translatedMove = translateMove(move)[count];
                if (translatedMove != null)
                    fakeCube = recreateCube(fakeCube, translatedMove);
            }
        }
    }

    private void shortenSolverMoves() {
        boolean flag = true;
        boolean flag2 = true;
        while (flag && flag2) {
            for (int count = 0; count < solverMoves.size() - 2; count ++) {
                if (solverMoves.get(count).equals(solverMoves.get(count + 1)) && solverMoves.get(count).equals(solverMoves.get(count + 2))) {
                    if (solverMoves.get(count).charAt(solverMoves.get(count).length() - 1) == '\'')
                        solverMoves.set(count, solverMoves.get(count).substring(0, 1));
                    else
                        solverMoves.set(count, solverMoves.get(count) + '\'');
                    for (int count2 = 0; count2 < 2; count2 ++)
                        solverMoves.remove(count + 1);
                    flag = true;
                }
                else
                    flag = false;
            }
            for (int count = 0; count < solverMoves.size() - 1; count ++)
                if (solverMoves.get(count).charAt(0) == solverMoves.get(count + 1).charAt(0) && solverMoves.get(count).length() != solverMoves.get(count + 1).length()) {
                    for (int count2 = 0; count2 < 2; count2 ++)
                        solverMoves.remove(count);
                    flag2 = true;
                }
                else
                    flag2 = false;
        }
    }

    private void whiteCross() {
        int initWhiteFace = -1;
        for (countFace = 0; countFace < FACES; countFace ++) {
            PhongMaterial material = (PhongMaterial) fakeCube[centralPieces[countFace][0]][centralPieces[countFace][1]][centralPieces[countFace][2]][countFace].getMaterial();
            if (material.getDiffuseMap().equals(colors[0].getDiffuseMap()))
                initWhiteFace = countFace;
        }
        switch (initWhiteFace) {
            case 0:
                addToSolverMoves("x'");
                break;
            case 1:
                addToSolverMoves("x'", "x'");
                break;
            case 2:
                addToSolverMoves("x");
                break;
            case 4:
                addToSolverMoves("z'");
                break;
            case 5:
                addToSolverMoves("z");
                break;
        }

        for (int count = 0; count < 4; count ++) {
            int edgePiecePos = -1;
            stopCount = 0;
            do {
                stopCount ++;
                int[] pieceXYZFF = findEdgePiece(fakeCube, colors[0], colors[switchFaces[2][count]]);
                for (int count2 = 0; count2 < edgePieces.length; count2 ++)
                    if (pieceXYZFF[0] == edgePieces[count2][0] && pieceXYZFF[1] == edgePieces[count2][1] && pieceXYZFF[2] == edgePieces[count2][2]) {
                        edgePiecePos = count2;
                        break;
                    }
                switch (edgePiecePos) {
                    case 0:
                        if (pieceXYZFF[3] == 0)
                            addToSolverMoves("F", "E", "F");
                        break;
                    case 1:
                        addToSolverMoves("F", "F");
                        break;
                    case 2:
                        addToSolverMoves("U", "U");
                        break;
                    case 3:
                        addToSolverMoves("B", "B");
                        break;
                    case 4:
                        addToSolverMoves("F'");
                        break;
                    case 5:
                        addToSolverMoves("F");
                        break;
                    case 6:
                        addToSolverMoves("E'");
                        break;
                    case 7:
                        addToSolverMoves("E");
                        break;
                    case 8:
                        addToSolverMoves("L'", "F'");
                        break;
                    case 9:
                        addToSolverMoves("U'", "F", "F");
                        break;
                    case 10:
                        addToSolverMoves("U", "F", "F");
                        break;
                    case 11:
                        addToSolverMoves("R", "F");
                        break;
                }
            } while (edgePiecePos != 0 && stopCount < 10);
            addToSolverMoves("y");
        }

        for (int count = 0; count < 4; count ++) {
            PhongMaterial material = (PhongMaterial) fakeCube[1][0][2][0].getMaterial();
            PhongMaterial material2 = (PhongMaterial) fakeCube[1][1][2][0].getMaterial();
            if (material.getDiffuseMap().equals(material2.getDiffuseMap())) break;
            else addToSolverMoves("E");
        }
    }

    private void intuitiveF2L() {
        for (int count = 0; count < 4; count ++) {
            int[] pieceXYZFFF;
            int cornerPiecePos = -1, finishCount = 0;
            stopCount = 0;
            do {
                stopCount ++;
                pieceXYZFFF = findCornerPiece(fakeCube, colors[0], colors[switchFaces[2][count]], colors[switchFaces[2][count + 1]]);
                for (int count2 = 0; count2 < cornerPieces.length; count2 ++)
                    if (pieceXYZFFF[0] == cornerPieces[count2][0] && pieceXYZFFF[1] == cornerPieces[count2][1] && pieceXYZFFF[2] == cornerPieces[count2][2]) {
                        cornerPiecePos = count2;
                        break;
                    }
                switch (cornerPiecePos) {
                    case 0:
                        addToSolverMoves("L'", "U'", "L");
                        break;
                    case 1:
                        addToSolverMoves("U'");
                        break;
                    case 2:
                        if (pieceXYZFFF[3] == 0 || pieceXYZFFF[3] == 1)
                            addToSolverMoves("R", "U", "U", "R'", "U'");
                        finishCount ++;
                        break;
                    case 3:
                        addToSolverMoves("R", "U", "R'");
                        break;
                    case 4:
                        addToSolverMoves("L", "U", "L'");
                        break;
                    case 5:
                    case 6:
                        addToSolverMoves("U");
                        break;
                    case 7:
                        addToSolverMoves("R'", "U", "R");
                        break;
                }
            } while (finishCount < 3 && stopCount < 10);
            int edgePiecePos = -1;
            stopCount = 0;
            boolean finishFlag = false;
            do {
                stopCount ++;
                int[] pieceXYZFF = findEdgePiece(fakeCube, colors[switchFaces[2][count]], colors[switchFaces[2][count + 1]]);
                for (int count2 = 0; count2 < edgePieces.length; count2 ++)
                    if (pieceXYZFF[0] == edgePieces[count2][0] && pieceXYZFF[1] == edgePieces[count2][1] && pieceXYZFF[2] == edgePieces[count2][2]) {
                        edgePiecePos = count2;
                        break;
                    }
                switch (edgePiecePos) {
                    case 1:
                        if (pieceXYZFF[3] == pieceXYZFFF[4])
                            addToSolverMoves("U'", "F'", "U", "F");
                        else
                            addToSolverMoves("U", "F'", "U", "U", "F", "U'", "R", "U", "R'");
                        finishFlag = true;
                        break;
                    case 2:
                        if (pieceXYZFF[4] == pieceXYZFFF[5])
                            addToSolverMoves("U", "F'", "U", "U", "F", "U", "U", "F'", "U", "F");
                        else
                            addToSolverMoves("R", "U", "R'");
                        finishFlag = true;
                        break;
                    case 4:
                        addToSolverMoves("U'", "L'", "U", "L");
                        break;
                    case 5:
                        addToSolverMoves("U", "R", "U", "R'", "U", "U");
                        break;
                    case 6:
                        addToSolverMoves("B", "U", "B'", "U'");
                        break;
                    case 7:
                        addToSolverMoves("B'", "U", "B", "U'");
                        break;
                    case 9:
                        addToSolverMoves("U", "U", "R", "U'", "R'", "U'");
                        break;
                    case 10:
                        addToSolverMoves("U'", "R", "U'", "R'", "U");
                        break;
                }
            } while (!finishFlag && stopCount < 10);
            addToSolverMoves("y");
        }
    }

    private void twoLookOLL() {
        int[] uEdgePieces = {1, 9, 2, 10};
        boolean[] situation;
        int countEdgePieces;
        stopCount = 0;
        boolean finishFlag = false;
        do {
            stopCount ++;
            situation = new boolean[]{false, false, false, false};
            countEdgePieces = 0;
            for (int count = 0; count < uEdgePieces.length; count ++) {
                PhongMaterial material = (PhongMaterial) fakeCube[edgePieces[uEdgePieces[count]][0]][edgePieces[uEdgePieces[count]][1]][edgePieces[uEdgePieces[count]][2]][1].getMaterial();
                if (material.getDiffuseMap().equals(colors[2].getDiffuseMap())) {
                    situation[count] = true;
                    countEdgePieces ++;
                }
            }
            switch (countEdgePieces) {
                case 0:
                case 1:
                    addToSolverMoves("F", "R", "U", "R'", "U'", "F'", "F", "R", "U", "R'", "U'", "F'");
                    break;
                case 2:
                case 3:
                    if (situation[0] && situation[2])
                        addToSolverMoves("U", "F", "R", "U", "R'", "U'", "F'");
                    else if (situation[1] && situation[3])
                        addToSolverMoves("F", "R", "U", "R'", "U'", "F'");
                    else if (situation[0] && situation[3])
                        addToSolverMoves("F", "R", "U", "R'", "U'", "F'");
                    else if (situation[0] && situation[1])
                        addToSolverMoves("U'", "F", "R", "U", "R'", "U'", "F'");
                    else if (situation[1] && situation[2])
                        addToSolverMoves("U", "U", "F", "R", "U", "R'", "U'", "F'");
                    else if (situation[2] && situation[3])
                        addToSolverMoves("U", "F", "R", "U", "R'", "U'", "F'");
                    break;
                case 4:
                    finishFlag = true;
                    break;
            }
        } while (!finishFlag && stopCount < 10);

        PhongMaterial[] materials = new PhongMaterial[4];
        String[][] instructions = {
                new String[]{"R", "U", "R'", "U", "R", "U", "U", "R'"},
                new String[]{"R", "U", "U", "R'", "U'", "R", "U'", "R'"},
                new String[]{"x'", "D", "R", "U'", "R'", "D'", "R", "U", "R'", "x"},
                new String[]{"x'", "R", "U'", "R'", "D", "R", "U", "R'", "D'", "x"},
                new String[]{"R", "R", "D", "R'", "U", "U", "R", "D'", "R'", "U", "U", "R'"},
                new String[]{"R", "U", "R'", "U", "R", "U'", "R'", "U", "R", "U", "U", "R'"},
                new String[]{"R", "U", "U", "R", "R", "U'", "R", "R", "U'", "R", "R", "U", "U", "R"},
        };
        boolean situation2Flag = false;
        stopCount = 0;
        do {
            stopCount ++;
            boolean checkFlag = true;
            for (countX = 0; countX < CUBE_DIMENSION; countX ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++) {
                    PhongMaterial material = (PhongMaterial) fakeCube[countX][2][countZ][1].getMaterial();
                    if (!material.getDiffuseMap().equals(colors[2].getDiffuseMap()))
                        checkFlag = false;
            }
            if (checkFlag) break;

            for (int count2 = 0; count2 < 7; count2 ++) {
                situation2Flag = true;
                switch (count2) {
                    case 0:
                        materials[0] = (PhongMaterial) fakeCube[0][2][2][1].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[0][2][0][2].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[2][2][0][5].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][0].getMaterial();
                        break;
                    case 1:
                        materials[0] = (PhongMaterial) fakeCube[2][2][0][1].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[0][2][0][4].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[2][2][2][5].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[0][2][2][0].getMaterial();
                        break;
                    case 2:
                        materials[0] = (PhongMaterial) fakeCube[0][2][0][1].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[2][2][0][1].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[0][2][2][4].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][5].getMaterial();
                        break;
                    case 3:
                        materials[0] = (PhongMaterial) fakeCube[2][2][0][1].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[0][2][2][1].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[0][2][0][2].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][5].getMaterial();
                        break;
                    case 4:
                        materials[0] = (PhongMaterial) fakeCube[0][2][0][1].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[2][2][0][1].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[0][2][2][0].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][0].getMaterial();
                        break;
                    case 5:
                        materials[0] = (PhongMaterial) fakeCube[0][2][0][4].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[2][2][0][5].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[0][2][2][4].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][5].getMaterial();
                        break;
                    case 6:
                        materials[0] = (PhongMaterial) fakeCube[0][2][0][4].getMaterial();
                        materials[1] = (PhongMaterial) fakeCube[2][2][0][2].getMaterial();
                        materials[2] = (PhongMaterial) fakeCube[0][2][2][4].getMaterial();
                        materials[3] = (PhongMaterial) fakeCube[2][2][2][0].getMaterial();
                        break;
                }
                for (PhongMaterial material : materials)
                    if (!material.getDiffuseMap().equals(colors[2].getDiffuseMap()))
                        situation2Flag = false;
                if (situation2Flag) {
                    addToSolverMoves(instructions[count2]);
                    break;
                }
            }
            if (!situation2Flag)
                addToSolverMoves("U");
        } while (!situation2Flag && stopCount < 10);
    }

    private void twoLookPLL() {
        PhongMaterial material, refMaterial, refMaterial2;
        int[] uCornerPieces = {1, 5, 6, 2};
        int cornerPiecePos = -1;
        int[] pieceXYZ;
        pieceXYZ = Arrays.copyOfRange(findCornerPiece(fakeCube, colors[2], colors[1], colors[4]), 0, 3);
        for (int count = 0; count < uCornerPieces.length; count ++)
            if (Arrays.equals(pieceXYZ, cornerPieces[uCornerPieces[count]]))
                cornerPiecePos = count;
        switch (cornerPiecePos) {
            case 1:
                addToSolverMoves("U'");
                break;
            case 2:
                addToSolverMoves("U", "U");
                break;
            case 3:
                addToSolverMoves("U");
                break;
        }
        ArrayList<Integer> warningCornerPiecesPos = new ArrayList<>();
        pieceXYZ = Arrays.copyOfRange(findCornerPiece(fakeCube, colors[2], colors[4], colors[3]), 0, 3);
        if (!Arrays.equals(pieceXYZ, cornerPieces[uCornerPieces[1]]))
            warningCornerPiecesPos.add(1);
        pieceXYZ = Arrays.copyOfRange(findCornerPiece(fakeCube, colors[2], colors[3], colors[5]), 0, 3);
        if (!Arrays.equals(pieceXYZ, cornerPieces[uCornerPieces[2]]))
            warningCornerPiecesPos.add(2);
        pieceXYZ = Arrays.copyOfRange(findCornerPiece(fakeCube, colors[2], colors[5], colors[1]), 0, 3);
        if (!Arrays.equals(pieceXYZ, cornerPieces[uCornerPieces[3]]))
            warningCornerPiecesPos.add(3);
        switch (warningCornerPiecesPos.size()) {
            case 2:
                if (warningCornerPiecesPos.get(0) == 1 && warningCornerPiecesPos.get(1) == 2)
                    addToSolverMoves("y", "R", "U", "R'", "U'", "R'", "F", "R", "R", "U'", "R'", "U'", "R", "U", "R'", "F'", "y'");
                else if (warningCornerPiecesPos.get(0) == 2 && warningCornerPiecesPos.get(1) == 3)
                    addToSolverMoves("R", "U", "R'", "U'", "R'", "F", "R", "R", "U'", "R'", "U'", "R", "U", "R'", "F'");
                else if (warningCornerPiecesPos.get(0) == 1 && warningCornerPiecesPos.get(1) == 3)
                    addToSolverMoves("F", "R", "U'", "R'", "U'", "R", "U", "R'", "F'", "R", "U", "R'", "U'", "R'", "F", "R", "F'");
                break;
            case 3:
                material = (PhongMaterial) fakeCube[0][2][0][2].getMaterial();
                if (material.getDiffuseMap().equals(colors[5].getDiffuseMap()))
                    addToSolverMoves("x", "R'", "U", "R'", "D", "D", "R", "U'", "R'", "D", "D", "R", "R", "x'");
                else
                    addToSolverMoves("x", "R", "R", "D", "D", "R", "U", "R'", "D", "D", "R", "U'", "R", "x'");
                break;
        }

        int[] uEdgePieces = {1, 9, 2, 10};
        ArrayList<Integer> warningEdgePiecesPos = new ArrayList<>();
        for (int count = 0; count < uEdgePieces.length; count ++) {
            material = (PhongMaterial) fakeCube[edgePieces[uEdgePieces[count]][0]][edgePieces[uEdgePieces[count]][1]][edgePieces[uEdgePieces[count]][2]][switchFaces[1][count]].getMaterial();
            refMaterial = (PhongMaterial) fakeCube[edgePieces[uEdgePieces[count]][0]][edgePieces[uEdgePieces[count]][1] - 1][edgePieces[uEdgePieces[count]][2]][switchFaces[1][count]].getMaterial();
            if (!material.getDiffuseMap().equals(refMaterial.getDiffuseMap()))
                warningEdgePiecesPos.add(count);
        }
        switch (warningEdgePiecesPos.size()) {
            case 3:
                ArrayList<String> afterMove = new ArrayList<>();
                if (!warningEdgePiecesPos.contains(0)) {
                    addToSolverMoves("y", "y");
                    afterMove.addAll(Arrays.asList("y", "y"));
                }
                else if (!warningEdgePiecesPos.contains(1)) {
                    addToSolverMoves("y");
                    afterMove.add("y'");
                }
                else if (!warningEdgePiecesPos.contains(3)) {
                    addToSolverMoves("y'");
                    afterMove.add("y");
                }
                material = (PhongMaterial) fakeCube[1][2][2][0].getMaterial();
                refMaterial = (PhongMaterial) fakeCube[0][1][1][4].getMaterial();
                if (material.getDiffuseMap().equals(refMaterial.getDiffuseMap()))
                    addToSolverMoves("R", "R", "U", "R", "U", "R'", "U'", "R'", "U'", "R'", "U", "R'");
                else
                    addToSolverMoves("R", "U'", "R", "U", "R", "U", "R", "U'", "R'", "U'", "R", "R");
                addToSolverMoves(afterMove.toArray(new String[0]));
                break;
            case 4:
                material = (PhongMaterial) fakeCube[1][2][2][0].getMaterial();
                refMaterial = (PhongMaterial) fakeCube[0][1][1][4].getMaterial();
                refMaterial2 = (PhongMaterial) fakeCube[1][1][0][2].getMaterial();
                if (material.getDiffuseMap().equals(refMaterial.getDiffuseMap()))
                    addToSolverMoves("R'", "U'", "R", "U'", "R", "U", "R", "U'", "R'", "U", "R", "U", "R", "R", "U'", "R'", "U", "U");
                else if (material.getDiffuseMap().equals(refMaterial2.getDiffuseMap()))
                    addToSolverMoves("M", "M", "U", "M", "M", "U", "U", "M", "M", "U", "M", "M");
                else
                    addToSolverMoves("M", "M", "U", "M", "M", "U", "M'", "U", "U", "M", "M", "U", "U", "M'", "U", "U");
                break;
        }
    }

    private int[] findEdgePiece(Box[][][][] cube, PhongMaterial firstColor, PhongMaterial secondColor) {
        int count;
        int firstColorFace = -1, secondColorFace = -1;
        int countFirstColor, countSecondColor, countBgColor;
        for (count = 0; count < edgePieces.length; count ++) {
            countFirstColor = 0;
            countSecondColor = 0;
            countBgColor = 0;
            for (countFace = 0; countFace < FACES; countFace ++) {
                PhongMaterial material = (PhongMaterial) cube[edgePieces[count][0]][edgePieces[count][1]][edgePieces[count][2]][countFace].getMaterial();
                if (material.getDiffuseMap().equals(firstColor.getDiffuseMap())) {
                    firstColorFace = countFace;
                    countFirstColor ++;
                }
                if (material.getDiffuseMap().equals(secondColor.getDiffuseMap())) {
                    secondColorFace = countFace;
                    countSecondColor ++;
                }
                if (material.getDiffuseMap().equals(colors[6].getDiffuseMap()))
                    countBgColor ++;
            }
            if (countFirstColor == 1 && countSecondColor == 1 && countBgColor == FACES - 2) break;
        }
        return new int[] {edgePieces[count][0], edgePieces[count][1], edgePieces[count][2], firstColorFace, secondColorFace};
    }

    private int[] findCornerPiece(Box[][][][] cube, PhongMaterial firstColor, PhongMaterial secondColor, PhongMaterial thirdColor) {
        int count;
        int firstColorFace = -1, secondColorFace = -1, thirdColorFace = -1;
        int countFirstColor, countSecondColor, countThirdColor, countBgColor;
        for (count = 0; count < cornerPieces.length; count ++) {
            countFirstColor = 0;
            countSecondColor = 0;
            countThirdColor = 0;
            countBgColor = 0;
            for (countFace = 0; countFace < FACES; countFace ++) {
                PhongMaterial material = (PhongMaterial) cube[cornerPieces[count][0]][cornerPieces[count][1]][cornerPieces[count][2]][countFace].getMaterial();
                if (material.getDiffuseMap().equals(firstColor.getDiffuseMap())) {
                    firstColorFace = countFace;
                    countFirstColor ++;
                }
                if (material.getDiffuseMap().equals(secondColor.getDiffuseMap())) {
                    secondColorFace = countFace;
                    countSecondColor ++;
                }
                if (material.getDiffuseMap().equals(thirdColor.getDiffuseMap())) {
                    thirdColorFace = countFace;
                    countThirdColor ++;
                }
                if (material.getDiffuseMap().equals(colors[6].getDiffuseMap()))
                    countBgColor ++;
            }
            if (countFirstColor == 1 && countSecondColor == 1 && countThirdColor == 1 && countBgColor == FACES - 3) break;
        }
        return new int[] {cornerPieces[count][0], cornerPieces[count][1], cornerPieces[count][2], firstColorFace, secondColorFace, thirdColorFace};
    }

    private void cubeMouseControl() {
        cubeGroup.getTransforms().addAll(
                xRotate = new Rotate(20, Rotate.X_AXIS),
                yRotate = new Rotate(10, Rotate.Y_AXIS)
        );
        xRotate.angleProperty().bind(angleX);
        yRotate.angleProperty().bind(angleY);

        cubePanel.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX.get();
            anchorAngleY = angleY.get();
        });

        cubePanel.setOnMouseDragged(event -> {
            angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
            angleY.set(anchorAngleY + (anchorX - event.getSceneX()));
        });
    }

    private Box[][][][] cloneCube(Box[][][][] startCube, Box[][][][] newCube) {
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++) {
                        newCube[countX][countY][countZ][countFace] = new Box(CUBE_SIDE, CUBE_SIDE, CUBE_SIDE);
                        newCube[countX][countY][countZ][countFace].setTranslateX(startCube[countX][countY][countZ][countFace].getTranslateX());
                        newCube[countX][countY][countZ][countFace].setTranslateY(startCube[countX][countY][countZ][countFace].getTranslateY());
                        newCube[countX][countY][countZ][countFace].setTranslateZ(startCube[countX][countY][countZ][countFace].getTranslateZ());
                        newCube[countX][countY][countZ][countFace].setMaterial(startCube[countX][countY][countZ][countFace].getMaterial());
                    }
        return newCube;
    }

    private boolean compareTwoCubes(Box[][][][] cube, Box[][][][] cube2) {
        boolean flag = true;
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++) {
                        PhongMaterial material = (PhongMaterial) cube[countX][countY][countZ][countFace].getMaterial();
                        PhongMaterial material2 = (PhongMaterial) cube2[countX][countY][countZ][countFace].getMaterial();
                        if (!material.getDiffuseMap().equals(material2.getDiffuseMap()))
                            flag = false;
                    }
        return flag;
    }

    private void addCubeToCubeGroup() {
        for (countX = 0; countX < CUBE_DIMENSION; countX ++)
            for (countY = 0; countY < CUBE_DIMENSION; countY ++)
                for (countZ = 0; countZ < CUBE_DIMENSION; countZ ++)
                    for (countFace = 0; countFace < FACES; countFace ++)
                        cubeGroup.getChildren().add(cube[countX][countY][countZ][countFace]);
    }

    public static void main(String[] args) {
        launch(args);
    }
}