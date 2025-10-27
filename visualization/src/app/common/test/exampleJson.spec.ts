export const exampleJson = {
  "projectTreeRoots": [
    {
      "name": "de",
      "children": [
        {
          "name": "sots",
          "children": [
            {
              "name": "cellarsandcentaurs",
              "children": [
                {
                  "name": "domain",
                  "children": [
                    {
                      "name": "model",
                      "children": [
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.CreatureId",
                          "name": "CreatureId",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.CreatureId"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.CreatureType",
                          "name": "CreatureType",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.CreatureType"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.HitPoints",
                          "name": "HitPoints",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.HitPoints"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
                          "name": "ArmorClass",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.ArmorClass"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.application.CreatureFacade": {
                              "isCyclic": true,
                              "weight": 1
                            }
                          }
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.Speed",
                          "name": "Speed",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.Speed"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.Fightable",
                          "name": "Fightable",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.Fightable"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.SpeedType",
                          "name": "SpeedType",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.SpeedType"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.Creature",
                          "name": "Creature",
                          "children": [],
                          "level": 1,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.Creature"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                              "isCyclic": true,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.Speed": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.Fightable": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.application.CreatureFacade": {
                              "isCyclic": true,
                              "weight": 1
                            }
                          }
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
                          "name": "NoSuchCreatureException",
                          "children": [],
                          "level": 1,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                              "isCyclic": false,
                              "weight": 1
                            }
                          }
                        }
                      ],
                      "level": 0,
                      "containedLeaves": [
                        "de.sots.cellarsandcentaurs.domain.model.CreatureId",
                        "de.sots.cellarsandcentaurs.domain.model.CreatureType",
                        "de.sots.cellarsandcentaurs.domain.model.HitPoints",
                        "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
                        "de.sots.cellarsandcentaurs.domain.model.Speed",
                        "de.sots.cellarsandcentaurs.domain.model.Fightable",
                        "de.sots.cellarsandcentaurs.domain.model.SpeedType",
                        "de.sots.cellarsandcentaurs.domain.model.Creature",
                        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException"
                      ],
                      "containedInternalDependencies": {
                        "de.sots.cellarsandcentaurs.application.CreatureFacade": {
                          "isCyclic": true,
                          "weight": 2
                        },
                        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                          "isCyclic": false,
                          "weight": 2
                        },
                        "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                          "isCyclic": true,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.Speed": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.Fightable": {
                          "isCyclic": false,
                          "weight": 1
                        }
                      }
                    },
                    {
                      "name": "service",
                      "children": [
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.service.Creatures",
                          "name": "Creatures",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.service.Creatures"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.domain.model.Creature": {
                              "isCyclic": true,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                              "isCyclic": false,
                              "weight": 1
                            }
                          }
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.domain.service.CreatureService",
                          "name": "CreatureService",
                          "children": [],
                          "level": 1,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.domain.service.CreatureService"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                              "isCyclic": true,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.Creature": {
                              "isCyclic": true,
                              "weight": 1
                            }
                          }
                        }
                      ],
                      "level": 1,
                      "containedLeaves": [
                        "de.sots.cellarsandcentaurs.domain.service.Creatures",
                        "de.sots.cellarsandcentaurs.domain.service.CreatureService"
                      ],
                      "containedInternalDependencies": {
                        "de.sots.cellarsandcentaurs.domain.model.Creature": {
                          "isCyclic": true,
                          "weight": 2
                        },
                        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                          "isCyclic": true,
                          "weight": 1
                        }
                      }
                    }
                  ],
                  "level": 0,
                  "containedLeaves": [
                    "de.sots.cellarsandcentaurs.domain.model.CreatureId",
                    "de.sots.cellarsandcentaurs.domain.model.CreatureType",
                    "de.sots.cellarsandcentaurs.domain.model.HitPoints",
                    "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
                    "de.sots.cellarsandcentaurs.domain.model.Speed",
                    "de.sots.cellarsandcentaurs.domain.model.Fightable",
                    "de.sots.cellarsandcentaurs.domain.model.SpeedType",
                    "de.sots.cellarsandcentaurs.domain.model.Creature",
                    "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
                    "de.sots.cellarsandcentaurs.domain.service.Creatures",
                    "de.sots.cellarsandcentaurs.domain.service.CreatureService"
                  ],
                  "containedInternalDependencies": {
                    "de.sots.cellarsandcentaurs.application.CreatureFacade": {
                      "isCyclic": true,
                      "weight": 2
                    },
                    "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                      "isCyclic": false,
                      "weight": 3
                    },
                    "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                      "isCyclic": true,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Speed": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Fightable": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Creature": {
                      "isCyclic": true,
                      "weight": 2
                    },
                    "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                      "isCyclic": true,
                      "weight": 1
                    }
                  }
                },
                {
                  "name": "adapter",
                  "children": [
                    {
                      "name": "persistence",
                      "children": [
                        {
                          "leafId": "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
                          "name": "CreatureEntity",
                          "children": [],
                          "level": 0,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity"
                          ],
                          "containedInternalDependencies": {}
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
                          "name": "CreatureJpaRepository",
                          "children": [],
                          "level": 1,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
                              "isCyclic": false,
                              "weight": 1
                            }
                          }
                        },
                        {
                          "leafId": "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures",
                          "name": "PersistedCreatures",
                          "children": [],
                          "level": 2,
                          "containedLeaves": [
                            "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures"
                          ],
                          "containedInternalDependencies": {
                            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.Creature": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
                              "isCyclic": false,
                              "weight": 1
                            },
                            "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                              "isCyclic": false,
                              "weight": 1
                            }
                          }
                        }
                      ],
                      "level": 0,
                      "containedLeaves": [
                        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
                        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
                        "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures"
                      ],
                      "containedInternalDependencies": {
                        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
                          "isCyclic": false,
                          "weight": 2
                        },
                        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.Creature": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                          "isCyclic": false,
                          "weight": 1
                        }
                      }
                    }
                  ],
                  "level": 1,
                  "containedLeaves": [
                    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
                    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
                    "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures"
                  ],
                  "containedInternalDependencies": {
                    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
                      "isCyclic": false,
                      "weight": 2
                    },
                    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Creature": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                      "isCyclic": false,
                      "weight": 1
                    }
                  }
                },
                {
                  "name": "application",
                  "children": [
                    {
                      "leafId": "de.sots.cellarsandcentaurs.application.CreatureFacade",
                      "name": "CreatureFacade",
                      "children": [],
                      "level": 0,
                      "containedLeaves": [
                        "de.sots.cellarsandcentaurs.application.CreatureFacade"
                      ],
                      "containedInternalDependencies": {
                        "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
                          "isCyclic": true,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.Speed": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                          "isCyclic": true,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                          "isCyclic": false,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.Creature": {
                          "isCyclic": true,
                          "weight": 1
                        },
                        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                          "isCyclic": false,
                          "weight": 1
                        }
                      }
                    }
                  ],
                  "level": 1,
                  "containedLeaves": [
                    "de.sots.cellarsandcentaurs.application.CreatureFacade"
                  ],
                  "containedInternalDependencies": {
                    "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
                      "isCyclic": true,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Speed": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                      "isCyclic": true,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                      "isCyclic": false,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.Creature": {
                      "isCyclic": true,
                      "weight": 1
                    },
                    "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                      "isCyclic": false,
                      "weight": 1
                    }
                  }
                }
              ],
              "level": 0,
              "containedLeaves": [
                "de.sots.cellarsandcentaurs.domain.model.CreatureId",
                "de.sots.cellarsandcentaurs.domain.model.CreatureType",
                "de.sots.cellarsandcentaurs.domain.model.HitPoints",
                "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
                "de.sots.cellarsandcentaurs.domain.model.Speed",
                "de.sots.cellarsandcentaurs.domain.model.Fightable",
                "de.sots.cellarsandcentaurs.domain.model.SpeedType",
                "de.sots.cellarsandcentaurs.domain.model.Creature",
                "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
                "de.sots.cellarsandcentaurs.domain.service.Creatures",
                "de.sots.cellarsandcentaurs.domain.service.CreatureService",
                "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
                "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
                "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures",
                "de.sots.cellarsandcentaurs.application.CreatureFacade"
              ],
              "containedInternalDependencies": {
                "de.sots.cellarsandcentaurs.application.CreatureFacade": {
                  "isCyclic": true,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
                  "isCyclic": false,
                  "weight": 5
                },
                "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
                  "isCyclic": true,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.Speed": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.model.Fightable": {
                  "isCyclic": false,
                  "weight": 1
                },
                "de.sots.cellarsandcentaurs.domain.model.Creature": {
                  "isCyclic": true,
                  "weight": 4
                },
                "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.domain.service.Creatures": {
                  "isCyclic": true,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
                  "isCyclic": false,
                  "weight": 2
                },
                "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
                  "isCyclic": false,
                  "weight": 1
                },
                "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
                  "isCyclic": true,
                  "weight": 1
                }
              }
            }
          ],
          "level": 0,
          "containedLeaves": [
            "de.sots.cellarsandcentaurs.domain.model.CreatureId",
            "de.sots.cellarsandcentaurs.domain.model.CreatureType",
            "de.sots.cellarsandcentaurs.domain.model.HitPoints",
            "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
            "de.sots.cellarsandcentaurs.domain.model.Speed",
            "de.sots.cellarsandcentaurs.domain.model.Fightable",
            "de.sots.cellarsandcentaurs.domain.model.SpeedType",
            "de.sots.cellarsandcentaurs.domain.model.Creature",
            "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
            "de.sots.cellarsandcentaurs.domain.service.Creatures",
            "de.sots.cellarsandcentaurs.domain.service.CreatureService",
            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
            "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures",
            "de.sots.cellarsandcentaurs.application.CreatureFacade"
          ],
          "containedInternalDependencies": {
            "de.sots.cellarsandcentaurs.application.CreatureFacade": {
              "isCyclic": true,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
              "isCyclic": false,
              "weight": 5
            },
            "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
              "isCyclic": true,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.Speed": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.model.Fightable": {
              "isCyclic": false,
              "weight": 1
            },
            "de.sots.cellarsandcentaurs.domain.model.Creature": {
              "isCyclic": true,
              "weight": 4
            },
            "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.domain.service.Creatures": {
              "isCyclic": true,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
              "isCyclic": false,
              "weight": 2
            },
            "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
              "isCyclic": false,
              "weight": 1
            },
            "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
              "isCyclic": true,
              "weight": 1
            }
          }
        }
      ],
      "level": 0,
      "containedLeaves": [
        "de.sots.cellarsandcentaurs.domain.model.CreatureId",
        "de.sots.cellarsandcentaurs.domain.model.CreatureType",
        "de.sots.cellarsandcentaurs.domain.model.HitPoints",
        "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
        "de.sots.cellarsandcentaurs.domain.model.Speed",
        "de.sots.cellarsandcentaurs.domain.model.Fightable",
        "de.sots.cellarsandcentaurs.domain.model.SpeedType",
        "de.sots.cellarsandcentaurs.domain.model.Creature",
        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
        "de.sots.cellarsandcentaurs.domain.service.Creatures",
        "de.sots.cellarsandcentaurs.domain.service.CreatureService",
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
        "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures",
        "de.sots.cellarsandcentaurs.application.CreatureFacade"
      ],
      "containedInternalDependencies": {
        "de.sots.cellarsandcentaurs.application.CreatureFacade": {
          "isCyclic": true,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 5
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
          "isCyclic": true,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.Speed": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.model.Fightable": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Creature": {
          "isCyclic": true,
          "weight": 4
        },
        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.domain.service.Creatures": {
          "isCyclic": true,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
          "isCyclic": false,
          "weight": 2
        },
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
          "isCyclic": true,
          "weight": 1
        }
      }
    }
  ],
  "leaves": {
    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
      "id": "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository",
      "name": "CreatureJpaRepository",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/adapter/persistence/CreatureJpaRepository.java",
      "nodeType": "INTERFACE",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
          "isCyclic": false,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
      "id": "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity",
      "name": "CreatureEntity",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/adapter/persistence/CreatureEntity.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures": {
      "id": "de.sots.cellarsandcentaurs.adapter.persistence.PersistedCreatures",
      "name": "PersistedCreatures",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/adapter/persistence/PersistedCreatures.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureJpaRepository": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Creature": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.service.Creatures": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.adapter.persistence.CreatureEntity": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
          "isCyclic": false,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.application.CreatureFacade": {
      "id": "de.sots.cellarsandcentaurs.application.CreatureFacade",
      "name": "CreatureFacade",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/application/CreatureFacade.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Speed": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Creature": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.domain.model.Creature": {
      "id": "de.sots.cellarsandcentaurs.domain.model.Creature",
      "name": "Creature",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/Creature.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Speed": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Fightable": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.application.CreatureFacade": {
          "isCyclic": true,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
      "id": "de.sots.cellarsandcentaurs.domain.model.CreatureId",
      "name": "CreatureId",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/CreatureId.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.model.CreatureType": {
      "id": "de.sots.cellarsandcentaurs.domain.model.CreatureType",
      "name": "CreatureType",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/CreatureType.java",
      "nodeType": "ENUM",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
      "id": "de.sots.cellarsandcentaurs.domain.model.HitPoints",
      "name": "HitPoints",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/HitPoints.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.model.ArmorClass": {
      "id": "de.sots.cellarsandcentaurs.domain.model.ArmorClass",
      "name": "ArmorClass",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/ArmorClass.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.application.CreatureFacade": {
          "isCyclic": true,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.domain.model.Speed": {
      "id": "de.sots.cellarsandcentaurs.domain.model.Speed",
      "name": "Speed",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/Speed.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
      "id": "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException",
      "name": "NoSuchCreatureException",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/NoSuchCreatureException.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.domain.model.Fightable": {
      "id": "de.sots.cellarsandcentaurs.domain.model.Fightable",
      "name": "Fightable",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/Fightable.java",
      "nodeType": "ANNOTATION",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.model.SpeedType": {
      "id": "de.sots.cellarsandcentaurs.domain.model.SpeedType",
      "name": "SpeedType",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/model/SpeedType.java",
      "nodeType": "ENUM",
      "language": "JAVA",
      "dependencies": {}
    },
    "de.sots.cellarsandcentaurs.domain.service.Creatures": {
      "id": "de.sots.cellarsandcentaurs.domain.service.Creatures",
      "name": "Creatures",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/service/Creatures.java",
      "nodeType": "INTERFACE",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.domain.model.Creature": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.CreatureId": {
          "isCyclic": false,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.NoSuchCreatureException": {
          "isCyclic": false,
          "weight": 1
        }
      }
    },
    "de.sots.cellarsandcentaurs.domain.service.CreatureService": {
      "id": "de.sots.cellarsandcentaurs.domain.service.CreatureService",
      "name": "CreatureService",
      "physicalPath": "/Users/moritz.suckow/learning/codegraph/./JavaExample/src/main/java/de/sots/cellarsandcentaurs/domain/service/CreatureService.java",
      "nodeType": "CLASS",
      "language": "JAVA",
      "dependencies": {
        "de.sots.cellarsandcentaurs.domain.service.Creatures": {
          "isCyclic": true,
          "weight": 1
        },
        "de.sots.cellarsandcentaurs.domain.model.Creature": {
          "isCyclic": true,
          "weight": 1
        }
      }
    }
  }
}
