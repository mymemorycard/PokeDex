package com.example.pokedex

import com.example.pokedex.models.Ability
import com.example.pokedex.models.AbilityInfo
import com.example.pokedex.models.ApiResult
import com.example.pokedex.models.Form
import com.example.pokedex.models.PokeList
import com.example.pokedex.models.PokemonInfo
import com.example.pokedex.models.Species
import com.example.pokedex.models.Sprites
import com.example.pokedex.models.Stat
import com.example.pokedex.models.StatNane
import com.example.pokedex.models.Type
import com.example.pokedex.models.TypeName

internal fun apiResult(name: String): ApiResult =
    ApiResult(name = name, url = "https://pokeapi.co/api/v2/pokemon/$name/")

internal fun pokeList(
    next: String? = null,
    items: List<ApiResult>,
    count: Long = items.size.toLong(),
): PokeList = PokeList(count = count, next = next, previous = null, results = items)

internal fun pokemonInfo(name: String): PokemonInfo = PokemonInfo(
    abilities = listOf(Ability(AbilityInfo("a", "u"), false, 1)),
    baseExperience = 1,
    forms = listOf(Form("f", "u")),
    gameIndices = emptyList(),
    height = 1,
    heldItems = emptyList(),
    id = 1,
    isDefault = true,
    locationAreaEncounters = "",
    moves = emptyList(),
    name = name,
    order = 1,
    species = Species("s", "u"),
    sprites = Sprites(
        backDefault = "u", backFemale = null, backShiny = "u", backShinyFemale = null,
        frontDefault = "u", frontFemale = null, frontShiny = "u", frontShinyFemale = null,
    ),
    stats = listOf(Stat(10, 0, StatNane("hp", "u"))),
    types = listOf(Type(1, TypeName("normal", "u"))),
    weight = 1,
)
