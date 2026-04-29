//setup
@24463 // middle of third row from the bottom
D=A
@current_paddle_middle
M=D

// set paddle_row_start to the top row of the paddle
@paddle_row_start
M=D-1

// create a counter (i) with value 3
@3
D=A
@i
M=D

// set the value of ball_spawned to 0 (false) - changed later once it's spawned
@ball_spawned
M=0

// set current_ball_top to top middle of screen
@16399 // 16384+15 - middle of top row
D=A
@current_ball_top
M=D


(PADDLE_SETUP_LOOP)
    @paddle_row_start
    A=M

    // set three words in a row to black
    M=-1
    A=A+1
    M=-1
    A=A+1
    M=-1

    // move to next row
    @32
    D=A
    @paddle_row_start
    M=M+D

    // decrement counter
    @i
    M=M-1
    D=M

    // go to top of loop if not finished with all three rows yet
    @PADDLE_SETUP_LOOP
    D;JNE

(TOTAL_LOOP)

    (DELAY) // delay so it doesn't go too fast
        @i
        M=M+1
        D=M
        @5000
        D=D-A
        @DELAY
        D;JNE

    @i
    M=0

    // get key pressed
    @KBD
    D=M
    @key_pressed
    M=D

    // if the left arrow is pressed, move paddle left
    @130
    D=D-A    
    @MOVE_PADDLE_LEFT
    D;JEQ

    // if the right arrow is pressed, move paddle right
    @key_pressed
    D=M
    @132
    D=D-A    
    @MOVE_PADDLE_RIGHT
    D;JEQ

    (AFTER_PADDLE_MOVE)

    // if the ball has spawned, move it down a row
    @ball_spawned
    D=M
    @MOVE_BALL
    D;JNE

    // if the down arrow is pressed, spawn the ball
    // (if it has already been spawned, the function won't run and just return to AFTER_BALL)
    @key_pressed
    D=M
    @133
    D=D-A    
    @SPAWN_BALL
    D;JEQ
    
    (AFTER_BALL)

    @TOTAL_LOOP
    0;JMP
    
(MOVE_PADDLE_LEFT)

    // skip if at beginning of screen
    @24449
    D=A
    @current_paddle_middle
    D=D-M
    @AFTER_PADDLE_MOVE
    D;JEQ

    // set paddle_row_middle to current_paddle_middle
    @current_paddle_middle
    D=M
    @paddle_row_middle
    M=D

    // create a counter (i) with value 3
    @3
    D=A
    @i
    M=D

    (MOVE_LEFT_LOOP)
        @paddle_row_middle // set right word to white
        A=M+1
        M=0

        @paddle_row_middle // set left word to black
        A=M-1
        A=A-1
        M=-1

        // move to next row
        @32
        D=A
        @paddle_row_middle
        M=M+D

        // decrement counter
        @i
        M=M-1
        D=M

        // go to top of loop if not finished with all three rows yet
        @MOVE_LEFT_LOOP
        D;JNE

    // update current_paddle_middle
    @current_paddle_middle
    M=M-1

    // go back to main loop
    @AFTER_PADDLE_MOVE
    0;JMP


(MOVE_PADDLE_RIGHT)

    // skip if at end of screen
    @24478
    D=A
    @current_paddle_middle
    D=D-M
    @AFTER_PADDLE_MOVE
    D;JEQ

    // set paddle_row_middle to current_paddle_middle
    @current_paddle_middle
    D=M
    @paddle_row_middle
    M=D

    // create a counter (i) with value 3
    @3
    D=A
    @i
    M=D

    (MOVE_RIGHT_LOOP)
        @paddle_row_middle // set right word to black
        A=M+1
        A=A+1
        M=-1

        @paddle_row_middle // set left word to white
        A=M-1
        M=0

        // move to next row
        @32
        D=A
        @paddle_row_middle
        M=M+D

        // decrement counter
        @i
        M=M-1
        D=M

        // go to top of loop if not finished with all three rows yet
        @MOVE_RIGHT_LOOP
        D;JNE

    // update current_paddle_middle
    @current_paddle_middle
    M=M+1

    // go back to main loop
    @AFTER_PADDLE_MOVE
    0;JMP

(SPAWN_BALL)

    // if the ball has already been spawned, skip this
    @ball_spawned
    D=M
    @AFTER_BALL
    D;JNE

    // set current_ball_row to current_ball_top
    @current_ball_top
    D=M

    @current_ball_row
    M=D

    // create a counter (i) with value 4
    @4
    D=A
    @i
    M=D

    (SPAWN_BALL_LOOP)

        @960 // 4 pixels in the middle of the word are black, rest white is 960
        D=A

        @current_ball_row
        A=M // move to top row of ball
        M=D // set first word of ball to 960

        // moving the current row down
        @32
        D=A
        @current_ball_row
        M=M+D

        // decrement counter
        @i
        M=M-1
        D=M

        // go to top of loop if not finished with all three rows yet
        @SPAWN_BALL_LOOP
        D;JNE

    // set ball_spawned to 1 so this function doesn't run again
    @ball_spawned
    M=1

    @AFTER_BALL
    0;JMP

(MOVE_BALL)

    // end game if the ball is below the screen
    @24448
    D=A
    @current_ball_top
    D=D-M
    @ERASE_BALL
    D;JLT

    @current_ball_top
    D=M
    // set top row of ball to white
    A=M
    M=0

    // store the row below the ball to come back to it
    @row_below_ball
    M=D
    @128 // 32*4 - 4 rows down
    D=A
    @row_below_ball
    M=M+D

    // if the row below the ball already is black (ie, is the paddle), don't change it
    @row_below_ball
    A=M
    D=M+1
    @AFTER_BOTTOM_ROW_SET
    D;JEQ

    // set the row below the ball to the middle 4 pixels being black
    @960
    D=A
    @row_below_ball
    A=M
    M=D

    (AFTER_BOTTOM_ROW_SET)

    // move the stored location of the ball down a row
    @32
    D=A
    @current_ball_top
    M=M+D

    @AFTER_BALL
    0;JMP

(ERASE_BALL)

    // set current_ball_row to current_ball_top
    @current_ball_top
    D=M

    @current_ball_row
    M=D

    // create a counter (i) with value 4
    @4
    D=A
    @i
    M=D

    (ERASE_BALL_LOOP)

        // if the row below the ball is fully black (ie, is the paddle), don't change it
        @current_ball_row
        A=M
        D=M+1
        @AFTER_ERASE_ROW_SET
        D;JEQ

        @current_ball_row
        A=M // move to top row of ball
        M=0 // erase top row

        (AFTER_ERASE_ROW_SET)

        // moving the current row down
        @32
        D=A
        @current_ball_row
        M=M+D

        // decrement counter
        @i
        M=M-1
        D=M

        // go to top of loop if not finished with all four rows yet
        @ERASE_BALL_LOOP
        D;JNE

    @END_GAME_LOOP
    0;JMP


(END_GAME_LOOP)
    @END_GAME_LOOP
    0;JMP



    