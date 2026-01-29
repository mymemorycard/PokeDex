package com.example.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.pokedex.models.PokeAPIRepository
import com.example.pokedex.models.PokeRepository
import com.example.pokedex.models.PokemonViewModel
import com.example.pokedex.ui.theme.PokeDexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokeDexTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        App()
                    }
                }
            }
        }
    }
}

class PokemonViewModelFactory(
    private val repository: PokeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PokemonViewModel(repository) as T
    }
}

@Composable
fun App() {
    val repository = PokeAPIRepository()

    val pokemonViewModel: PokemonViewModel = viewModel(
        factory = PokemonViewModelFactory(repository)
    )

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "root"
    ) {
        pokeApiGraph(
            navController = navController,
            pokemonViewModel = pokemonViewModel
        )
    }
}