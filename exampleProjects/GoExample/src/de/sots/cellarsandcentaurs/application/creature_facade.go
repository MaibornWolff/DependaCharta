package application

import (
	"github.com/gorilla/mux"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/service"
	"net/http"
)

type CreatureFacade struct {
	creatureService *service.CreatureService
	router          *mux.Router
}

func NewCreatureFacade(creatureService *service.CreatureService) *CreatureFacade {
	facade := &CreatureFacade{
		creatureService: creatureService,
		router:          mux.NewRouter(),
	}
	facade.setupRoutes()
	return facade
}

func (cf *CreatureFacade) setupRoutes() {
	cf.router.HandleFunc("/creatures", cf.handleGetCreatures).Methods("GET")
	cf.router.HandleFunc("/creatures", cf.handleCreateCreature).Methods("POST")
	cf.router.HandleFunc("/creatures/{id}", cf.handleGetCreature).Methods("GET")
	cf.router.HandleFunc("/creatures/{id}", cf.handleUpdateCreature).Methods("PUT")
	cf.router.HandleFunc("/creatures/{id}", cf.handleDeleteCreature).Methods("DELETE")
}

func (cf *CreatureFacade) GetRouter() *mux.Router {
	return cf.router
}

func (cf *CreatureFacade) CreateStandardCreature() (*model.Creature, error) {
	id := model.NewCreatureId()
	creature := model.NewCreatureWithType(id, model.StandardCreatureType)

	armorClass, err := model.NewArmorClass(10)
	if err != nil {
		return nil, err
	}
	creature.SetArmorClass(armorClass)

	hitPoints, err := model.NewHitPoints(10)
	if err != nil {
		return nil, err
	}
	creature.SetHitPoints(hitPoints)

	walkingSpeed, err := model.NewSpeed(30)
	if err != nil {
		return nil, err
	}
	creature.AddSpeed(model.Walking, walkingSpeed)

	return creature, nil
}

func (cf *CreatureFacade) handleGetCreatures(w http.ResponseWriter, r *http.Request) {
	_, err := cf.creatureService.FindAll()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

func (cf *CreatureFacade) handleCreateCreature(w http.ResponseWriter, r *http.Request) {
	creature, err := cf.CreateStandardCreature()
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	err = cf.creatureService.Save(creature)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
}

func (cf *CreatureFacade) handleGetCreature(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]

	id, err := model.NewCreatureIdFromString(idStr)
	if err != nil {
		http.Error(w, "Invalid creature ID", http.StatusBadRequest)
		return
	}

	_, err = cf.creatureService.FindById(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

func (cf *CreatureFacade) handleUpdateCreature(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]

	id, err := model.NewCreatureIdFromString(idStr)
	if err != nil {
		http.Error(w, "Invalid creature ID", http.StatusBadRequest)
		return
	}

	creature, err := cf.creatureService.FindById(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusNotFound)
		return
	}

	err = cf.creatureService.Save(creature)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

func (cf *CreatureFacade) handleDeleteCreature(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]

	id, err := model.NewCreatureIdFromString(idStr)
	if err != nil {
		http.Error(w, "Invalid creature ID", http.StatusBadRequest)
		return
	}

	err = cf.creatureService.Delete(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
