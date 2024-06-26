# Sample script showing how to terminate trains used in operations
# Prints a list of trains, and then if a train was built, terminates
# the train by moving it to the end of its route
#
# Author: Daniel Boudreau, copyright 2010, 2024
# Part of the JMRI distribution

import jmri

class terminateTrains(jmri.jmrit.automat.AbstractAutomaton) :      
  def init(self):
    # get the train manager
    self.tm = jmri.InstanceManager.getDefault(jmri.jmrit.operations.trains.TrainManager)
    return

  def handle(self):
    # get a list of trains from the manager
    trainList = self.tm.getTrainsByIdList()
    print ('number of trains {}'.format(len(trainList))) 

    count = 1

    # show a list of trains
    for train in trainList :
      print ('train {} {}, {}'.format(count, train.getName(), train.getDescription())) 
      count = count + 1

    # now terminate trains that were built by moving them
    for train in trainList :
      if (train.isBuilt() == True):
         print ('train {} was built'.format(train.getName()))
         while (train.isBuilt() == True):
            print ('move train {}'.format(train.getName())) 
            train.move() 
         print ('train {} terminated'.format(train.getName()))

    return False              # all done, don't repeat again

terminateTrains().start()          # create one of these, and start it running
