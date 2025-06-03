package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val USER_NAME_KEY = stringPreferencesKey("user_name")

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var githubApi: GitHubService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()
        getAllReposByUserName()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    private fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        progressBar = findViewById(R.id.progressBar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            lifecycleScope.launch {
                saveUserLocal()
            }
        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private suspend fun saveUserLocal() {
        nomeUsuario.text?.let {
            if (it.trim().isNotEmpty()) {
                getAllReposByUserName()
                dataStore.edit { settings ->
                    settings[USER_NAME_KEY] = it.toString()
                }
            }
        }
    }

    private fun showUserName() {
        lifecycleScope.launch {
            dataStore.data.map { settings -> settings[USER_NAME_KEY] ?: "" }
                .collect(nomeUsuario::setText)
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder().baseUrl("https://api.github.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    private fun getAllReposByUserName() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                nomeUsuario.isEnabled = false
                val listResult =
                    githubApi.getAllRepositoriesByUser(nomeUsuario.text.toString())
                setupAdapter(listResult)
            } catch (ex: Exception) {
                ex.message?.let { Log.d("REQUEST", it) }
                showErrorMessage()
            } finally {
                progressBar.visibility = View.GONE
                nomeUsuario.isEnabled = true
            }
        }
    }

    private fun showErrorMessage() {
        Toast.makeText(this, R.string.ex_failed_get_repository, Toast.LENGTH_LONG).show();
    }

    // Metodo responsavel por realizar a configuracao do adapter
    private fun setupAdapter(list: List<Repository>) {

        val repositoryAdapter = RepositoryAdapter(list).apply {
            carItemLister = { openBrowser(it.htmlUrl) }
            btnShareLister = { shareRepositoryLink(it.htmlUrl) }
        }
//        listaRepositories.layoutManager = LinearLayoutManager(this)
        listaRepositories.adapter = repositoryAdapter
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    private fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio

    private fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                urlRepository.toUri()
            )
        )

    }

}