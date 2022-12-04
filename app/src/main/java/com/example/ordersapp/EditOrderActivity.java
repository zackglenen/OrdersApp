package com.example.ordersapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Objects;

public class EditOrderActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //Declare variables for all widgets
    Button btn_update, btn_delete, btn_backToMain;
    TextView tv_titleSingleOrder;
    EditText et_name, et_number, et_address;
    Spinner spinner_size;
    CheckBox cb_pepperoni, cb_olives, cb_bellPepper, cb_feta, cb_pineapple, cb_jalapeno;

    //Arrays to hold topping info
    MainActivity.Topping[] toppings;
    CheckBox[] checkBoxes;

    //Create string array for setting text
    String[] language;
    String[] spinner_items = new String[3];
    static String LANGUAGE_KEY = "language_key";

    //To set the size
    int size;
    MainActivity.Size currentSize;

    //To make sure toppings don't go over 3
    int numOfToppings = 0;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_order);

        //Grabbing id from the intent
        int id = Integer.parseInt(getIntent().getStringExtra("id"));

        //Relate all variables to their xml counterparts
        tv_titleSingleOrder = findViewById(R.id.tv_titleSingleOrder);
        btn_update = findViewById(R.id.btn_update);
        btn_delete = findViewById(R.id.btn_delete);

        btn_backToMain = findViewById(R.id.btn_backToMain);
        et_name = findViewById(R.id.et_name);
        et_number = findViewById(R.id.et_number);
        et_address = findViewById(R.id.et_address);
        spinner_size = findViewById(R.id.spinner_size);
        cb_pepperoni = findViewById(R.id.cb_pepperoni);
        cb_olives = findViewById(R.id.cb_olives);
        cb_bellPepper = findViewById(R.id.cb_bellPepper);
        cb_feta = findViewById(R.id.cb_feta);
        cb_pineapple = findViewById(R.id.cb_pineapple);
        cb_jalapeno = findViewById(R.id.cb_jalapeno);

        cb_pepperoni.setOnClickListener(onChecked);
        cb_olives.setOnClickListener(onChecked);
        cb_bellPepper.setOnClickListener(onChecked);
        cb_feta.setOnClickListener(onChecked);
        cb_pineapple.setOnClickListener(onChecked);
        cb_jalapeno.setOnClickListener(onChecked);

        spinner_size.setOnItemSelectedListener(this);

        //Set the arrays for toppings
        toppings = new MainActivity.Topping[3];
        checkBoxes = new CheckBox[]{
                cb_pepperoni,
                cb_olives,
                cb_bellPepper,
                cb_feta,
                cb_pineapple,
                cb_jalapeno
        };

        //Set adapter object
        DBAdapter adapter = new DBAdapter(this);

        //Set the text of onscreen widgets
        setText();

        //Code for creating and populating a spinner object
        ArrayAdapter ad = new ArrayAdapter(this, android.R.layout.simple_spinner_item, spinner_items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_size.setAdapter(ad);

        try{
            //Open adapter and get single order based on the passed id
            adapter.open();
            Cursor c = adapter.getOrder(id);

            if (c.moveToFirst()){
                //Grab all of the info in database and set accordingly with correct objects
                et_name.setText(c.getString(c.getColumnIndex("name")));
                et_number.setText(c.getString(c.getColumnIndex("number")));
                et_address.setText(c.getString(c.getColumnIndex("address")));
                String title = tv_titleSingleOrder.getText() + " " + String.valueOf(id);
                tv_titleSingleOrder.setText(title);
                size = c.getInt(c.getColumnIndex("size"));
                toppings[0] = MainActivity.Topping.values()[c.getInt(c.getColumnIndex("top1"))];
                toppings[1] = MainActivity.Topping.values()[c.getInt(c.getColumnIndex("top2"))];
                toppings[2] = MainActivity.Topping.values()[c.getInt(c.getColumnIndex("top3"))];
            }
            adapter.close();//Close the adapter
        }catch (SQLiteException e){
            e.printStackTrace();
        }catch (Exception e ){
            e.printStackTrace();
        }

        //Set the spinner based on number stored in database
        if (size == MainActivity.Size.SMALL.ordinal()){
            spinner_size.setSelection(MainActivity.Size.SMALL.ordinal());
        }else if (size == MainActivity.Size.MEDIUM.ordinal()){
            spinner_size.setSelection(MainActivity.Size.MEDIUM.ordinal());
        }else{
            spinner_size.setSelection(MainActivity.Size.LARGE.ordinal());
        }

        //Set the appropriate checkboxes to checked based on the numbers given
        for (int i = 0; i < checkBoxes.length; i++) {
            for (MainActivity.Topping topping : toppings) {
                if (i == (topping.ordinal() - 1)) {
                    checkBoxes[i].setChecked(true);
                    numOfToppings++;//Increment the number of toppings
                }
            }
        }

        btn_backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Send back to main activity
                Intent i = new Intent(EditOrderActivity.this, MainActivity.class);
                startActivity(i);
            }
        });//End Back to Main on click listener

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    adapter.open();//Open adapter
                    if (adapter.deleteOrder(id)){//Try to delete row based on ID and prompt user if successful
                        Toast.makeText(view.getContext(), "Delete successful.", Toast.LENGTH_SHORT).show();
                        adapter.close();
                        //if successful, close adapter and send back to the all orders activity
                        Intent i = new Intent(EditOrderActivity.this, OrderActivity.class);
                        startActivity(i);
                    }else {
                        Toast.makeText(view.getContext(), "Delete has failed.", Toast.LENGTH_SHORT).show();
                    }
                    adapter.close();//close the adapter
                }catch (SQLiteException e){
                    e.printStackTrace();
                }

            }
        });

        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if (validateText()){//Make sure all text is valid
                        checkToppings();//Populate toppings array with whatever is selected
                        adapter.open();//Open adapter and update the row based on the id. Prompt user with toast
                        if (adapter.updateOrder(id,et_name.getText().toString(),et_number.getText().toString(),
                                et_address.getText().toString(),currentSize.ordinal(),toppings[0].ordinal(),toppings[1].ordinal(),toppings[2].ordinal())){
                            Toast.makeText(view.getContext(), "Update successful.", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(view.getContext(), "Update has failed.", Toast.LENGTH_SHORT).show();
                        }
                        adapter.close();//close the adapter
                    }
                }catch (SQLiteException e){
                    e.printStackTrace();
                }

            }
        });
    }//End on create
    public View.OnClickListener onChecked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case (R.id.cb_pepperoni):
                    //if is checked and there is 3 toppings, set checkable to false and prompt user
                    if (cb_pepperoni.isChecked() && numOfToppings>=3){
                        cb_pepperoni.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();
                    }else{
                        //Else, if the user is checking or unchecking, add or subtract from the number of toppings appropriately
                        if (cb_pepperoni.isChecked()){
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                case (R.id.cb_olives):
                    if (cb_olives.isChecked() && numOfToppings>=3){
                        cb_olives.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();
                    }else{
                        if (cb_olives.isChecked()) {
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                case (R.id.cb_bellPepper):

                    if (cb_bellPepper.isChecked() && (numOfToppings>=3)){
                        cb_bellPepper.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();

                    }else{
                        if (cb_bellPepper.isChecked()) {
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                case (R.id.cb_feta):

                    if ( cb_feta.isChecked() && numOfToppings>=3){
                        cb_feta.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();

                    }else{
                        if (cb_feta.isChecked()) {
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                case (R.id.cb_pineapple):
                    if (cb_pineapple.isChecked() && numOfToppings>=3){
                        cb_pineapple.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();

                    }else {
                        if (cb_pineapple.isChecked()) {
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                case (R.id.cb_jalapeno):
                    if (cb_jalapeno.isChecked() && numOfToppings>=3){
                        cb_jalapeno.setChecked(false);
                        Toast.makeText(getBaseContext(), "Please only choose 3 toppings.", Toast.LENGTH_LONG).show();

                    }else{
                        if (cb_jalapeno.isChecked()) {
                            numOfToppings++;
                        } else {
                            numOfToppings--;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };//End on Checked

    public void setText() {
        //Create prefs and retrieve language data
        SharedPreferences prefs = getSharedPreferences("language", MODE_PRIVATE);

        //If the key is english, set the string array to the english one, otherwise french
        if (Objects.equals(prefs.getString(LANGUAGE_KEY, ""), "english")) {
            language = getResources().getStringArray(R.array.english);
        } else {
            language = getResources().getStringArray(R.array.french);
        }
        //Set all text values of widgets
        spinner_items[0] = language[7];
        spinner_items[1] = language[8];
        spinner_items[2] = language[9];
        cb_pepperoni.setText(language[10]);
        cb_olives.setText(language[11]);
        cb_bellPepper.setText(language[12]);
        cb_feta.setText(language[13]);
        cb_pineapple.setText(language[14]);
        cb_jalapeno.setText(language[15]);
        btn_backToMain.setText(language[19]);
        tv_titleSingleOrder.setText(language[20]);
        btn_update.setText(language[21]);
        btn_delete.setText(language[22]);

    }//End Set Text Method

    public boolean validateText(){
        boolean willPass = true;
        String numPattern = "\\d{10}|(?:\\d{3}-){2}\\d{4}|\\(\\d{3}\\)\\d{3}-?\\d{4}";//Phone number regex

        //if name is not between 0 and 70 characters throw false and prompt with toast
        if (et_name.getText().toString().length() <= 0 ||et_name.getText().toString().length() >= 70 ){
            Toast.makeText(getBaseContext(), "Please make sure text is between 1 & 70 characters.", Toast.LENGTH_LONG).show();
            willPass = false;
        }

        //If number doesnt match regex
        if (!et_number.getText().toString().matches(numPattern)){
            Toast.makeText(getBaseContext(), "Please make sure phone number is a valid 10 digit North American number.", Toast.LENGTH_LONG).show();
            willPass = false;
        }

        //if address is not between 0 and 70 characters throw false and prompt with toast
        if (et_address.getText().toString().length() <= 0 ||et_address.getText().toString().length() >= 70 ){
            Toast.makeText(getBaseContext(), "Please make sure text is between 1 & 70 characters.", Toast.LENGTH_LONG).show();
            willPass = false;
        }

        return willPass;
    }

    public void checkToppings(){
        //Loop through each checkbox and see if its checked
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                //if checked, loop through each toppings
                for (int j = 0; j < toppings.length; j++) {
                    if (toppings[j] == null){
                        //If the topping it null, assign it to the appropriate enum value for topping based on checkbox
                        toppings[j] = MainActivity.Topping.values()[i+1];
                        break;//break through inner loop
                    }
                }
            }
        }
        //Loop through each toppings in case fewer than three were selected
        //Choose No topping for any remaining nulls
        for (int i = 0; i < toppings.length; i++) {
            if (toppings[i]==null){
                toppings[i] = MainActivity.Topping.NO_TOPPING;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //Set the current size in spinner
        if (l == 0){
            currentSize = MainActivity.Size.SMALL;
        }else if (l == 1){
            currentSize = MainActivity.Size.MEDIUM;
        }else{
            currentSize = MainActivity.Size.LARGE;
        }
    }//Spinner method

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }//Spinner method



}//End Main Method