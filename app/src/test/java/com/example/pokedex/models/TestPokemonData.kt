package com.example.pokedex.models

fun sampleApiResult(name: String = "pikachu") = ApiResult(
    name = name,
    url = "https://pokeapi.co/api/v2/pokemon/$name"
)

fun samplePokeList(vararg names: String) = PokeList(
    count = names.size.toLong(),
    next = null,
    previous = null,
    results = names.map(::sampleApiResult)
)

fun samplePokemonInfo(name: String = "pikachu") = PokemonInfo(
    abilities = listOf(
        Ability(
            ability = AbilityInfo(
                name = "static",
                url = "https://pokeapi.co/api/v2/ability/9/"
            ),
            isHidden = false,
            slot = 1
        )
    ),
    baseExperience = 112,
    forms = listOf(
        Form(
            name = name,
            url = "https://pokeapi.co/api/v2/pokemon-form/$name/"
        )
    ),
    gameIndices = listOf(
        Index(
            gameIndex = 84,
            version = Version(
                name = "red",
                url = "https://pokeapi.co/api/v2/version/1/"
            )
        )
    ),
    height = 4,
    heldItems = listOf(
        HeldItem(
            item = Item(
                name = "oran-berry",
                url = "https://pokeapi.co/api/v2/item/132/"
            ),
            versionDetails = listOf(
                VersionDetail(
                    rarity = 50,
                    version = Version2(
                        name = "red",
                        url = "https://pokeapi.co/api/v2/version/1/"
                    )
                )
            )
        )
    ),
    id = 25,
    isDefault = true,
    locationAreaEncounters = "https://pokeapi.co/api/v2/pokemon/$name/encounters",
    moves = listOf(
        Mfe(
            move = Move(
                name = "thunder-shock",
                url = "https://pokeapi.co/api/v2/move/84/"
            ),
            versionGroupDetails = listOf(
                VersionGroupDetail(
                    levelLearnedAt = 1,
                    moveLearnMethod = MoveLearnMethod(
                        name = "level-up",
                        url = "https://pokeapi.co/api/v2/move-learn-method/1/"
                    ),
                    order = 1,
                    versionGroup = VersionGroup(
                        name = "red-blue",
                        url = "https://pokeapi.co/api/v2/version-group/1/"
                    )
                )
            )
        )
    ),
    name = name,
    order = 35,
    species = Species(
        name = name,
        url = "https://pokeapi.co/api/v2/pokemon-species/$name/"
    ),
    sprites = Sprites(
        backDefault = "https://img/$name/back.png",
        backFemale = null,
        backShiny = "https://img/$name/back-shiny.png",
        backShinyFemale = null,
        frontDefault = "https://img/$name/front.png",
        frontFemale = null,
        frontShiny = "https://img/$name/front-shiny.png",
        frontShinyFemale = null
    ),
    stats = listOf(
        Stat(
            baseStat = 35,
            effort = 0,
            stat = StatNane(
                name = "speed",
                url = "https://pokeapi.co/api/v2/stat/6/"
            )
        )
    ),
    types = listOf(
        Type(
            slot = 1,
            type = TypeName(
                name = "electric",
                url = "https://pokeapi.co/api/v2/type/13/"
            )
        )
    ),
    weight = 60
)
