package com.example.ac2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText edtId, edtTitulo, edtDiretor, edtAno, edtNota;
    private Spinner spGenero;
    private CheckBox cbCinema;
    private RatingBar rbEstrelas;

    private ListView lvFilmes;
    private ArrayAdapter<String> listAdapter;
    private final ArrayList<String> linhas = new ArrayList<>();
    private final ArrayList<String> docIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        edtId      = findViewById(R.id.edtId);
        edtTitulo  = findViewById(R.id.edtTitulo);
        edtDiretor = findViewById(R.id.edtDiretor);
        edtAno     = findViewById(R.id.edtAno);
        edtNota    = findViewById(R.id.edtNota);
        spGenero   = findViewById(R.id.spGenero);
        cbCinema   = findViewById(R.id.cbCinema);
        rbEstrelas = findViewById(R.id.rbEstrelas);

        spGenero.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Ação","Drama","Comédia","Ficção","Suspense","Terror","Romance","Animação"}
        ));

        lvFilmes = findViewById(R.id.lvFilmes);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, linhas);
        lvFilmes.setAdapter(listAdapter);

        lvFilmes.setOnItemClickListener((parent, view, position, id) -> {
            String docId = docIds.get(position);
            filmesRef().document(docId).get().addOnSuccessListener(doc -> {
                if (!doc.exists()) { Toast.makeText(this, "Documento não encontrado", Toast.LENGTH_SHORT).show(); return; }
                edtId.setText(doc.getId());
                edtTitulo.setText(String.valueOf(doc.get("titulo")==null?"":doc.get("titulo")));
                edtDiretor.setText(String.valueOf(doc.get("diretor")==null?"":doc.get("diretor")));
                Object anoObj = doc.get("ano");
                edtAno.setText(anoObj==null ? "" : String.valueOf(((Number)anoObj).intValue()));
                Object notaObj = doc.get("nota");
                edtNota.setText(notaObj==null ? "" : String.valueOf(((Number)notaObj).intValue()));
                String genero = String.valueOf(doc.get("genero")==null?"":doc.get("genero"));
                if (genero != null) {
                    for (int i=0;i<spGenero.getCount();i++){
                        if (genero.equals(spGenero.getItemAtPosition(i))) { spGenero.setSelection(i); break; }
                    }
                }
                Object est = doc.get("estrelas");
                rbEstrelas.setRating(est==null?0:((Number)est).intValue());
                Object cinema = doc.get("viuNoCinema");
                cbCinema.setChecked(cinema instanceof Boolean && (Boolean) cinema);
            });
        });
    }

    public void sair(View v) {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private CollectionReference filmesRef() {
        String uid = mAuth.getCurrentUser().getUid();
        return db.collection("users").document(uid).collection("filmes");
    }

    public void salvarFilme(View v) {
        String titulo = edtTitulo.getText().toString().trim();
        if (titulo.isEmpty()) { edtTitulo.setError(getString(R.string.obrigatorio)); return; }

        Map<String, Object> filme = new HashMap<>();
        filme.put("titulo", titulo);
        filme.put("diretor", edtDiretor.getText().toString().trim());
        try { filme.put("ano", Integer.parseInt(edtAno.getText().toString().trim())); } catch (Exception e) { filme.put("ano", null); }
        filme.put("genero", spGenero.getSelectedItem()==null?null:spGenero.getSelectedItem().toString());
        try { filme.put("nota", Integer.parseInt(edtNota.getText().toString().trim())); } catch (Exception e) { filme.put("nota", null); }
        filme.put("viuNoCinema", cbCinema.isChecked());
        filme.put("estrelas", Math.round(rbEstrelas.getRating()));

        filmesRef().add(filme)
                .addOnSuccessListener(docRef -> {
                    edtId.setText(docRef.getId());
                    Toast.makeText(this, getString(R.string.msg_salvo_id, docRef.getId()), Toast.LENGTH_LONG).show();
                    listarFilmes(null); // já atualiza a lista na tela
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.err_generico, e.getMessage()), Toast.LENGTH_LONG).show();
                    Log.e("FIREBASE", "Erro ao salvar", e);
                });
    }

    public void atualizarFilme(View v) {
        String id = edtId.getText().toString().trim();
        if (id.isEmpty()) { edtId.setError(getString(R.string.informe_id)); return; }

        Map<String, Object> filme = new HashMap<>();
        if (!edtTitulo.getText().toString().trim().isEmpty())  filme.put("titulo", edtTitulo.getText().toString().trim());
        if (!edtDiretor.getText().toString().trim().isEmpty()) filme.put("diretor", edtDiretor.getText().toString().trim());
        try { filme.put("ano", Integer.parseInt(edtAno.getText().toString().trim())); } catch (Exception ignored) {}
        if (spGenero.getSelectedItem()!=null) filme.put("genero", spGenero.getSelectedItem().toString());
        try { filme.put("nota", Integer.parseInt(edtNota.getText().toString().trim())); } catch (Exception ignored) {}
        filme.put("viuNoCinema", cbCinema.isChecked());
        filme.put("estrelas", Math.round(rbEstrelas.getRating()));

        filmesRef().document(id).set(filme)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, R.string.msg_atualizado, Toast.LENGTH_SHORT).show();
                    listarFilmes(null);
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.err_generico, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    public void excluirFilme(View v) {
        String id = edtId.getText().toString().trim();
        if (id.isEmpty()) { edtId.setError(getString(R.string.informe_id)); return; }

        filmesRef().document(id).delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, R.string.msg_excluido, Toast.LENGTH_SHORT).show();
                    edtId.setText("");
                    listarFilmes(null);
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.err_generico, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    public void listarFilmes(View v) {
        filmesRef().get()
                .addOnSuccessListener(snap -> {
                    linhas.clear();
                    docIds.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String id = doc.getId();
                        String titulo = String.valueOf(doc.get("titulo")==null?"(sem título)":doc.get("titulo"));
                        String genero = String.valueOf(doc.get("genero")==null?"-":doc.get("genero"));
                        String ano = doc.get("ano")==null?"-":String.valueOf(((Number)doc.get("ano")).intValue());
                        String nota = doc.get("nota")==null?"-":String.valueOf(((Number)doc.get("nota")).intValue());
                        boolean cinema = doc.get("viuNoCinema") instanceof Boolean && (Boolean) doc.get("viuNoCinema");
                        String linha = titulo + " (" + ano + ") • " + nota + "★ • " + genero + (cinema ? " • Cinema" : "");
                        linhas.add(linha);
                        docIds.add(id);
                    }
                    listAdapter.notifyDataSetChanged();
                    Toast.makeText(this, getString(R.string.msg_total, snap.size()), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.err_generico, e.getMessage()), Toast.LENGTH_LONG).show());
    }
}
