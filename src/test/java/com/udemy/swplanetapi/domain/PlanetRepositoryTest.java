package com.udemy.swplanetapi.domain;

import static com.udemy.swplanetapi.common.PlanetConstants.PLANET;
import static com.udemy.swplanetapi.common.PlanetConstants.TATOOINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Example;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
public class PlanetRepositoryTest {

    @Autowired
    private PlanetRepository planetRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @AfterEach
    public void afterEach() {
        PLANET.setId(null);
    }

    @Test
    public void createPlanet_WithValidDate_ReturnsPlanet() {
        Planet planet = planetRepository.save(PLANET);
        Planet sut = testEntityManager.find(Planet.class, planet.getId());

        System.out.println(planet);

        assertThat(sut).isNotNull();
        assertThat(sut.getName()).isEqualTo(PLANET.getName());
        assertThat(sut.getClimate()).isEqualTo(PLANET.getClimate());
        assertThat(sut.getTerrain()).isEqualTo(PLANET.getTerrain());
    }

    private static Stream<Arguments> providesInvalidPlanets(){
        return Stream.of(
            Arguments.of(new Planet("", "temperate", "grasslands, mountains")),
            Arguments.of(new Planet("Alderaan", "", "grasslands, mountains")),
            Arguments.of(new Planet("Alderaan", "temperate", "")),
            Arguments.of(new Planet("", "", "")),
            Arguments.of(new Planet(null, "temperate", "grasslands, mountains")),
            Arguments.of(new Planet("Alderaan", null, "grasslands, mountains")),
            Arguments.of(new Planet("Alderaan", "temperate", null)),
            Arguments.of(new Planet(null, null, null)),
            Arguments.of(new Planet(null, null, "grasslands, mountains")),
            Arguments.of(new Planet(null, "temperate", null)),
            Arguments.of(new Planet("Alderaan", null, null))
        );
    }

    @ParameterizedTest
    @MethodSource("providesInvalidPlanets")
    public void createPlanet_WithInvalidData_ThrowsException(Planet planet) {

        assertThatThrownBy(() -> planetRepository.save(planet)).isInstanceOf(RuntimeException.class);

    }

    @Test
    public void createPlanet_WithExistingName_ThrowsException() {
        Planet planet = testEntityManager.persistFlushFind(PLANET);
        testEntityManager.detach(planet);
        planet.setId(null);

        assertThatThrownBy(() -> planetRepository.save(planet)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void getPlanet_ByExistingId_ReturnsPlanet() {
        Planet planet = testEntityManager.persistFlushFind(PLANET);
        Optional<Planet> planetExpected = planetRepository.findById(planet.getId());

        assertThat(planetExpected).isNotEmpty();
        assertThat(planetExpected.get()).isEqualTo(planet);
    }

    @Test
    public void getPlanet_ByNonExistingId_ReturnsNotFound() {
        Optional<Planet> planetExpected = planetRepository.findById(1L);

        assertThat(planetExpected).isEmpty();

    }

    @Test
    public void getPlanet_ByExistingName_ReturnsPlanet() {
        Planet planet = testEntityManager.persistFlushFind(PLANET);
        Optional<Planet> planetExpected = planetRepository.findByName(planet.getName());

        assertThat(planetExpected).isNotEmpty();
        assertThat(planetExpected.get()).isEqualTo(planet);
    }

    @Test
    public void getPlanet_ByNonExistingName_ReturnsNotFound() {
        Optional<Planet> planetExpected = planetRepository.findByName("Tatooine");

        assertThat(planetExpected).isEmpty();
    }

    @Sql(scripts = "/import_planets.sql")
    @Test
    public void listPlanets_ReturnsFilteredPlanets() throws Exception {
        Example<Planet> queryWithoutFilters = QueryBuilder.makeQuery(new Planet());
        Example<Planet> queryWithFilters = QueryBuilder
                .makeQuery(new Planet(TATOOINE.getClimate(), TATOOINE.getTerrain()));

        List<Planet> responseWithoutFilters = planetRepository.findAll(queryWithoutFilters);
        List<Planet> responseWithFilters = planetRepository.findAll(queryWithFilters);

        assertThat(responseWithoutFilters).isNotEmpty();
        assertThat(responseWithoutFilters).hasSize(3);

        assertThat(responseWithFilters).isNotEmpty();
        assertThat(responseWithFilters).hasSize(1);
        assertThat(responseWithFilters.get(0)).isEqualTo(TATOOINE);
    }

    @Test
    public void listPlanets_ReturnsNoPlanets() throws Exception {
        Example<Planet> query = QueryBuilder.makeQuery(new Planet());
        List<Planet> response = planetRepository.findAll(query);
        assertThat(response).isEmpty();
    }

    @Test
    public void removePlanet_WithExistingId_RemovesPlanetFromDatabase() throws Exception {
        Planet planet = testEntityManager.persistFlushFind(PLANET);

        planetRepository.deleteById(planet.getId());

        Planet removedPlanet = testEntityManager.find(Planet.class, planet.getId());
        assertThat(removedPlanet).isNull();

    }
}
