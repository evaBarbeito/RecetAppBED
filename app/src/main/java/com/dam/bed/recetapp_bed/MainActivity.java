package com.dam.bed.recetapp_bed;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListner;

    ListView listView;
    ListViewAdapterRecipe adapter;
    TextView noRecipesFound;

    ArrayList<Recipe> arrayList = new ArrayList<>();

    String diet = "";
    String email, replacedEmail;
    ArrayList<String> ingreUser, ingreRecipe;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListner);


        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.action_recipe);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_recipe:
                        startActivity(new Intent(getBaseContext(), MainActivity.class));
                        break;

                    case R.id.action_ingredient:
                        startActivity(new Intent(getBaseContext(), SelectIngredients.class));
                        break;

                    case R.id.action_cuest:
                        startActivity(new Intent(getBaseContext(), Cuestionario.class));
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.recipelist);

        //FIREBASE
        mAuth = FirebaseAuth.getInstance();
        mAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser()==null)
                {
                    startActivity(new Intent(MainActivity.this, Login.class));
                }
            }
        };
        try {
            listView = findViewById(R.id.listViewRecipeList);
            noRecipesFound = findViewById(R.id.noRecipes);

            mAuth = FirebaseAuth.getInstance();
            email = mAuth.getCurrentUser().getEmail();
            //replacedEmail = email.replace("@", "\\").replace(".", "-");
            replacedEmail = SingletonRecetApp.getInstance().replaceEmail(email);

            ValueEventListener valueEventListenerUser = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user.isQuest()) {
                        diet = user.getDiet();

                        ingreUser = user.getIngredients();

                        ValueEventListener valueEventListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    Recipe recipe = ds.getValue(Recipe.class);
                                    ingreRecipe = recipe.getIngredients();

                                    // Si la lista de ingredientes del usuario esta vacia,
                                    // inicializamos ingreUser como una nueva ArrayLista para que no de null
                                    if (ingreUser == null) ingreUser = new ArrayList<>();

                                    if (Collections.disjoint(ingreUser, ingreRecipe)) {
                                        if (diet.equals("Omniv")) {
                                            arrayList.add(recipe);
                                        } else if (diet.equals("Vegetarian")) {
                                            if (!recipe.getType().equals("Omniv")) {
                                                arrayList.add(recipe);
                                            }
                                        } else if (diet.equals("Vegan")) {
                                            if (recipe.getType().equals("Vegan")) {
                                                arrayList.add(recipe);
                                            }
                                        }
                                    }
                                }
                                // TODO mostrar al usuario que no se ha encontrado ninguna receta
                                System.out.println("Arraylist size: " + arrayList.size());
                                if (arrayList.size() == 0) {
//                                listView.setVisibility(View.GONE);
                                    noRecipesFound.setVisibility(View.VISIBLE);
                                    System.out.println("Text no recipes: " + noRecipesFound.getText());
                                } else {
                                    noRecipesFound.setVisibility(View.GONE);
                                    listView.setVisibility(View.VISIBLE);
                                }
                                adapter = new ListViewAdapterRecipe(getBaseContext(), arrayList);
                                listView.setAdapter(adapter);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                System.out.println("error" + databaseError.getMessage());
                            }
                        };
                        FirebaseDatabase.getInstance().getReference("Recipes/").addValueEventListener(valueEventListener);

                    } else {
                        startActivity(new Intent(MainActivity.this, Cuestionario.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println("error" + databaseError.getMessage());
                }
            };
            FirebaseDatabase.getInstance().getReference("users/" + replacedEmail).addValueEventListener(valueEventListenerUser);


        } catch (Exception e) {
            System.out.println("************Error");
        }
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.logout) {
            mAuth.signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
